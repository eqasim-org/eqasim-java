package org.eqasim.core.components.network_calibration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpecialRegionsReader {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final List<Geometry> regions = new ArrayList<>();

    public SpecialRegionsReader(String filename){
        File file = new File(filename);
        if (!file.exists()){
            throw new RuntimeException("File not found: " + filename);
        }
        readFile(file);
    }

    public boolean isInSpecialRegion(Coord coordinates){
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(coordinates.getX(), coordinates.getY()));

        for (Geometry region : regions) {
            // covers includes boundary points, which is typically expected for region filters
            if (region.covers(point)) {
                return true;
            }
        }

        return false;
    }

    public int getNumberOfRegions(){
        return regions.size();
    }

    public Set<Id<Link>> getLinksInSpecialRegions(Network network) {
        if (getNumberOfRegions() == 0) {
            throw new RuntimeException("No special regions defined to add to network");
        }

        return network.getLinks().values().parallelStream()
                .filter(l -> isInSpecialRegion(l.getFromNode().getCoord()) && isInSpecialRegion(l.getToNode().getCoord()))
                .map(Link::getId)
                .collect(Collectors.toSet());
    }

    private void readFile(File file){
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(file);
            parseGeoJsonObject(root);

            if (regions.isEmpty()) {
                throw new RuntimeException("No Polygon or MultiPolygon geometries found in file: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read GeoJSON file: " + file.getAbsolutePath(), e);
        }
    }

    private void parseGeoJsonObject(JsonNode node) {
        String type = getType(node);

        switch (type) {
            case "FeatureCollection" -> {
                JsonNode featuresNode = node.get("features");
                if (featuresNode == null || !featuresNode.isArray()) {
                    throw new RuntimeException("Invalid FeatureCollection: missing features array");
                }
                for (JsonNode featureNode : featuresNode) {
                    parseGeoJsonObject(featureNode);
                }
            }
            case "Feature" -> {
                JsonNode geometryNode = node.get("geometry");
                if (geometryNode != null && !geometryNode.isNull()) {
                    parseGeoJsonObject(geometryNode);
                }
            }
            case "Polygon" -> regions.add(parsePolygon(node.get("coordinates")));
            case "MultiPolygon" -> parseMultiPolygon(node.get("coordinates"));
            default -> throw new RuntimeException("Unsupported GeoJSON type: " + type);
        }
    }

    private void parseMultiPolygon(JsonNode coordinatesNode) {
        if (coordinatesNode == null || !coordinatesNode.isArray()) {
            throw new RuntimeException("Invalid MultiPolygon coordinates");
        }

        for (JsonNode polygonNode : coordinatesNode) {
            regions.add(parsePolygon(polygonNode));
        }
    }

    private Geometry parsePolygon(JsonNode coordinatesNode) {
        if (coordinatesNode == null || !coordinatesNode.isArray() || coordinatesNode.size() == 0) {
            throw new RuntimeException("Invalid Polygon coordinates");
        }

        LinearRing shell = GEOMETRY_FACTORY.createLinearRing(parseRing(coordinatesNode.get(0)));
        LinearRing[] holes = new LinearRing[Math.max(0, coordinatesNode.size() - 1)];

        for (int i = 1; i < coordinatesNode.size(); i++) {
            holes[i - 1] = GEOMETRY_FACTORY.createLinearRing(parseRing(coordinatesNode.get(i)));
        }

        return GEOMETRY_FACTORY.createPolygon(shell, holes);
    }

    private Coordinate[] parseRing(JsonNode ringNode) {
        if (ringNode == null || !ringNode.isArray() || ringNode.size() < 4) {
            throw new RuntimeException("Invalid Polygon ring: expected at least 4 coordinates");
        }

        List<Coordinate> coordinates = new ArrayList<>();

        for (JsonNode positionNode : ringNode) {
            if (positionNode == null || !positionNode.isArray() || positionNode.size() < 2) {
                throw new RuntimeException("Invalid coordinate position in ring");
            }
            coordinates.add(new Coordinate(positionNode.get(0).asDouble(), positionNode.get(1).asDouble()));
        }

        Coordinate first = coordinates.get(0);
        Coordinate last = coordinates.get(coordinates.size() - 1);

        if (!first.equals2D(last)) {
            coordinates.add(new Coordinate(first.x, first.y));
        }

        return coordinates.toArray(new Coordinate[0]);
    }

    private String getType(JsonNode node) {
        if (node == null || !node.isObject()) {
            throw new RuntimeException("Invalid GeoJSON object");
        }

        JsonNode typeNode = node.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            throw new RuntimeException("Invalid GeoJSON object: missing type");
        }

        return typeNode.asText();
    }

}
