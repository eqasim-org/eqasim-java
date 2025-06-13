package org.eqasim.core.scenario.freeflow;

import java.util.LinkedList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

public class FreeflowConfigurator {
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    public enum LinkType {
        major, intermediate, minor
    }

    public enum CrossingType {
        major, minor, equal, none
    }

    public record LinkRecord(LinkType linkType, CrossingType crossingType) {
    }

    static public FreeflowConfigurator create(Network network) {
        IdMap<Link, LinkRecord> links = new IdMap<>(Link.class);

        for (Link link : network.getLinks().values()) {
            links.put(link.getId(), new LinkRecord(decideLinkType(link), decideCrossingType(link)));
        }

        return new FreeflowConfigurator(links, network);
    }

    private final TravelTime delegate = new FreeSpeedTravelTime();

    private final Network network;
    private final IdMap<Link, LinkRecord> records;

    private FreeflowConfigurator(IdMap<Link, LinkRecord> records, Network network) {
        this.records = records;
        this.network = network;
    }

    private record AreaRecord(Geometry geometry, double factor) {
    }

    public TravelTime getTravelTime(FreeflowConfiguration configuration) {
        IdMap<Link, Double> factors = new IdMap<>(Link.class);
        IdMap<Link, Double> delays = new IdMap<>(Link.class);

        List<AreaRecord> areas = new LinkedList<>();
        for (FreeflowConfiguration.Area area : configuration.areas) {
            try {
                Geometry geometry = new WKTReader().read(area.wkt);
                areas.add(new AreaRecord(geometry, area.factor));
            } catch (ParseException e) {
                throw new IllegalStateException(e);
            }
        }

        for (var entry : records.entrySet()) {
            LinkRecord linkRecord = entry.getValue();

            double roadFactor = switch (linkRecord.linkType) {
                case major -> configuration.majorFactor;
                case intermediate -> configuration.intermediateFactor;
                case minor -> configuration.minorFactor;
                default -> 1.0;
            };

            double delay = switch (linkRecord.crossingType) {
                case major -> configuration.majorCrossingPenalty_s;
                case equal -> configuration.equalCrossingPenalty_s;
                case minor -> configuration.minorCrossingPenalty_s;
                default -> 0.0;
            };

            if (configuration.areas.size() > 0) {
                Link link = network.getLinks().get(entry.getKey());

                Point point = geometryFactory
                        .createPoint(new Coordinate(link.getCoord().getX(), link.getCoord().getY()));

                for (AreaRecord area : areas) {
                    if (area.geometry.covers(point)) {
                        roadFactor *= area.factor;
                    }
                }
            }

            if (roadFactor != 1.0) {
                factors.put(entry.getKey(), roadFactor);
            }

            if (delay != 0.0) {
                delays.put(entry.getKey(), delay);
            }
        }

        return (link, time, person,
                vehicle) -> delegate.getLinkTravelTime(link, time, person, vehicle)
                        * factors.getOrDefault(link.getId(), 1.0)
                        + delays.getOrDefault(link.getId(), 0.0);
    }

    public void apply(Network network, FreeflowConfiguration configuration) {
        TravelTime travelTime = getTravelTime(configuration);

        for (Link link : network.getLinks().values()) {
            LinkRecord record = records.get(link.getId());

            link.setFreespeed(link.getLength() / travelTime.getLinkTravelTime(link, 0.0, null, null));
            link.getAttributes().putAttribute("freeflow:linkType", record.linkType.toString());
            link.getAttributes().putAttribute("freeflow:crossingType", record.crossingType.toString());
        }
    }

    private static LinkType decideLinkType(Link link) {
        String osm = (String) link.getAttributes().getAttribute("osm:way:highway");

        if (osm != null) {
            if (osm.contains("motorway") || osm.contains("trunk") || osm.contains("primary")) {
                return LinkType.major;
            } else if (osm.contains("secondary") || osm.contains("tertiary")) {
                return LinkType.intermediate;
            }
        }

        return LinkType.minor;
    }

    private static CrossingType decideCrossingType(Link link) {
        if (link.getToNode().getInLinks().size() == 1) { // straight road or diverge
            return CrossingType.none;
        } else {
            double maximumCapacity = Double.NEGATIVE_INFINITY;
            double minimumCapacity = Double.POSITIVE_INFINITY;

            for (Link inlink : link.getToNode().getInLinks().values()) {
                maximumCapacity = Math.max(maximumCapacity, inlink.getCapacity());
                minimumCapacity = Math.min(minimumCapacity, inlink.getCapacity());
            }

            if (maximumCapacity == minimumCapacity) {
                return CrossingType.equal;
            } else if (link.getCapacity() == maximumCapacity) {
                return CrossingType.major;
            } else {
                return CrossingType.minor;
            }
        }
    }
}
