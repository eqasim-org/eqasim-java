package org.eqasim.examples.corsica_parking.components.parking;

import com.google.inject.Inject;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ParkingListener implements StartParkingSearchEventHandler, PersonLeavesVehicleEventHandler,
        PersonArrivalEventHandler, IterationEndsListener {

    private final Map<Id<Person>, Double> personIdParkingSearchStart = new HashMap<>();
    private final Map<Id<Person>, Double> personIdParkingSearchTimes = new HashMap<>();
    private final Map<Id<Node>, double[]> arrivalCount = new HashMap<>();
    private final Map<Id<Node>, double[]> parkingSearchTimes = new HashMap<>();

    private final Vehicle2DriverEventHandler vehicle2DriverEventHandler;
    private final RoadNetwork network;
    private final double startTime;
    private final double endTime;
    private final double interval;
    private final int numberOfBins;
    private double queryRadius;

    @ Inject
    public ParkingListener(Vehicle2DriverEventHandler vehicle2DriverEventHandler, RoadNetwork network,
                           double startTime, double endTime, double interval, double queryRadius) {
        this.vehicle2DriverEventHandler = vehicle2DriverEventHandler;

        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
        this.numberOfBins = getTimeBin(startTime, endTime, interval);
        this.network = network;
        this.queryRadius = queryRadius;

        for (Id<Node> nodeId : network.getNodes().keySet()) {
            parkingSearchTimes.putIfAbsent(nodeId, new double[numberOfBins]);
            arrivalCount.putIfAbsent(nodeId, new double[numberOfBins]);
        }
    }

    @Override
    public void handleEvent(StartParkingSearchEvent event) {
        double time = event.getTime();
        Id<Person> personId = vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId());
        personIdParkingSearchStart.putIfAbsent(personId, time);
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        Id<Person> personId = event.getPersonId();
        // check if person is in parking search phase
        if (personIdParkingSearchStart.containsKey(personId)) {
            double searchTime = event.getTime() - personIdParkingSearchStart.remove(personId);
            personIdParkingSearchTimes.put(personId, searchTime);
        }
    }

    @Override
    // not interaction activity
    public void handleEvent(PersonArrivalEvent event) {
        Id<Person> personId = event.getPersonId();

        if (personIdParkingSearchTimes.containsKey(personId)) {
            if (event.getLegMode().equals(TransportMode.walk)) {

                // extract relevant search variables
                double parkingSearchTime = personIdParkingSearchTimes.remove(personId);
                double arrivalTime = event.getTime();
                Coord destinationCoord = network.getLinks().get(event.getLinkId()).getFromNode().getCoord();

                // get affected nodes within query radius
                Collection<Node> nodes = network.getNearestNodes(destinationCoord, queryRadius);
                for (Node node : nodes) {
                    Id<Node> nodeId = node.getId();

                    // compute time bin
                    int timeBin = getTimeBin(startTime, arrivalTime, interval);

                    parkingSearchTimes.get(nodeId)[timeBin] += parkingSearchTime;
                    arrivalCount.get(nodeId)[timeBin] += 1;

                }
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {

        // compute average values
        for (Id<Node> nodeId : arrivalCount.keySet()) {
            for (int i = 0; i<numberOfBins; i++) {
                if (arrivalCount.get(nodeId)[i] > 0) {
                    parkingSearchTimes.get(nodeId)[i] /= arrivalCount.get(nodeId)[i];
                }
            }
        }
    }

    @Override
    public void reset(int iteration) {
        parkingSearchTimes.clear();
        arrivalCount.clear();

        for (Id<Node> nodeId : network.getNodes().keySet()) {
            parkingSearchTimes.putIfAbsent(nodeId, new double[numberOfBins]);
            arrivalCount.putIfAbsent(nodeId, new double[numberOfBins]);
        }
    }

    public Map<Id<Node>, double[]> getParkingSearchTimes() {
        return parkingSearchTimes;
    }

    public double[] getParkingSearchTimesAtCoord(Coord coord) {
        Id<Node> nodeId = network.getNearestNode(coord).getId();
        return parkingSearchTimes.get(nodeId);
    }

    public double getParkingSearchTimeAtCoordAtTime(Coord coord, double time) {
        int timeBin = getTimeBin(startTime, time, interval);
        return getParkingSearchTimesAtCoord(coord)[timeBin];
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    private int getTimeBin(double startTime, double endTime, double interval) {
        return (int) Math.floor((endTime - startTime) / interval);
    }

}
