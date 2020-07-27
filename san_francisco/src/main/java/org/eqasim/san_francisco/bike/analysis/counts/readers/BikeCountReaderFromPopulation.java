package org.eqasim.san_francisco.bike.analysis.counts.readers;

import org.apache.log4j.Logger;
import org.eqasim.san_francisco.bike.analysis.counts.CountUtils;
import org.eqasim.san_francisco.bike.analysis.counts.items.CountItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BikeCountReaderFromPopulation {
    public static final Logger log = Logger.getLogger(BikeCountReaderFromPopulation.class);

    public double binSize;
    public int numberBins;
    public double bikeSpeed;

    public BikeCountReaderFromPopulation(double bikeSpeed, double binSize) {
        this.bikeSpeed = bikeSpeed;
        this.binSize = binSize;
        this.numberBins = (int) (3600 * 24 / binSize);
    }

    public Map<Id<Link>, List<CountItem>> read(Population population, Network network) {

        Map<Id<Link>, List<CountItem>> map = new HashMap<>();

        int totalLegs = 0;
        int genericLegs = 0;

        for (Person person : population.getPersons().values()) {
            for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {

                if (planElement instanceof Leg) {
                    Leg leg = (Leg) planElement;

                    if (leg.getMode().equals(TransportMode.bike)) {

                        totalLegs++;

                        double departureTime = leg.getDepartureTime();

                        // if the leg is teleported, only consider start and end links
                        if (leg.getRoute().getRouteType().equals("generic")) {

                            log.warn("generic leg for person id " + person.getId().toString());
                            genericLegs++;

                            Id<Link> startLinkId = leg.getRoute().getStartLinkId();
                            Id<Link> endLinkId = leg.getRoute().getEndLinkId();

                            // check if we have already added the links to map and if not, do so
                            map.putIfAbsent(startLinkId, CountUtils.createNewCountItemsList(startLinkId,
                                    TransportMode.bike, numberBins, binSize));
                            map.putIfAbsent(endLinkId, CountUtils.createNewCountItemsList(endLinkId,
                                    TransportMode.bike, numberBins, binSize));

                            // get time bin corresponding to each links enter time
                            int startTimeBin = CountUtils.getTimeBin(departureTime, numberBins, binSize);
                            double endLinkLength = network.getLinks().get(endLinkId).getLength();
                            double endLinkTravelTime = endLinkLength / bikeSpeed;
                            double endLinkEnterTime = departureTime + leg.getTravelTime() - endLinkTravelTime;
                            int endTimeBin = CountUtils.getTimeBin(endLinkEnterTime, numberBins, binSize);

                            // update counts
                            map.get(startLinkId).get(startTimeBin).increase(1);
                            map.get(endLinkId).get(endTimeBin).increase(1);

                        } else {
                            String routeDescription = leg.getRoute().getRouteDescription();
                            String[] linkIds = routeDescription.split(" ");
                            double linkEnterTime = departureTime;

                            for (String id : linkIds) {

                                Id<Link> linkId = Id.createLinkId(id);

                                // check if we have already added this link to map and if not, do so
                                map.putIfAbsent(linkId, CountUtils.createNewCountItemsList(linkId,
                                        TransportMode.bike, numberBins, binSize));

                                // get time bin corresponding to link enter time
                                int timeBin = CountUtils.getTimeBin(linkEnterTime, numberBins, binSize);

                                // update count
                                map.get(linkId).get(timeBin).increase(1);

                                // update link enter time for next link based on bike speed and link length
                                double linkLength = network.getLinks().get(linkId).getLength();
                                double linkTravelTime = linkLength / bikeSpeed;
                                linkEnterTime += linkTravelTime;
                            }
                        }

                    }
                }

            }
        }

        log.info(genericLegs + " of " + totalLegs + " are generic bike legs");

        return map;
    }
}
