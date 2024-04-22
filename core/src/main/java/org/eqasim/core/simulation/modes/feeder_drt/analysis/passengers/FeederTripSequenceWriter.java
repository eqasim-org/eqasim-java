package org.eqasim.core.simulation.modes.feeder_drt.analysis.passengers;

import java.io.*;

public class FeederTripSequenceWriter {

    private final FeederTripSequenceListener feederTripSequenceListener;

    public FeederTripSequenceWriter(FeederTripSequenceListener feederTripSequenceListener) {
        this.feederTripSequenceListener = feederTripSequenceListener;
    }


    public void writeTripItems(File path) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

        writer.write(String.join(";", new String[] { //
                "person_id", //

                "origin_link_id", //
                "origin_x", //
                "origin_y", //

                "destination_link_id", //
                "destination_x", //
                "destination_y", //

                "access_vehicle_id", //
                "egress_vehicle_id", //

                "access_departure_time",
                "egress_departure_time",

                "access_arrival_time",
                "egress_arrival_time",

                "access_transit_stop_id",
                "egress_transit_stop_id",

                "access_transit_line_id",
                "egress_transit_line_id",

                "access_transit_route_id",
                "egress_transit_route_id"
        }) + "\n");

        for (FeederTripSequenceItem item : feederTripSequenceListener.getItemsList()) {
            writer.write(String.join(";", new String[] { //
                    String.valueOf(item.personId), //

                    String.valueOf(item.originLink.getId()), //
                    String.valueOf(item.originLink.getCoord().getX()), //
                    String.valueOf(item.originLink.getCoord().getY()), //

                    String.valueOf(item.destinationLink.getId()), //
                    String.valueOf(item.destinationLink.getCoord().getX()), //
                    String.valueOf(item.destinationLink.getCoord().getY()), //

                    item.accessVehicleId == null ? "NaN" : String.valueOf(item.accessVehicleId),
                    item.egressVehicleId == null ? "NaN" : String.valueOf(item.egressVehicleId),

                    Double.isNaN(item.accessDepartureTime) ? "NaN" : String.valueOf(item.accessDepartureTime),
                    Double.isNaN(item.egressDepartureTime) ? "NaN" : String.valueOf(item.egressDepartureTime),

                    Double.isNaN(item.accessArrivalTime) ? "NaN" : String.valueOf(item.accessArrivalTime),
                    Double.isNaN(item.egressArrivalTime) ? "NaN" : String.valueOf(item.egressArrivalTime),

                    String.valueOf(item.accessVehicleId),
                    String.valueOf(item.egressVehicleId),

                    String.valueOf(item.accessTransitLineId),
                    String.valueOf(item.egressTransitLineId),

                    String.valueOf(item.accessTransitRouteId),
                    String.valueOf(item.egressTransitRouteId),
            }) + "\n");
        }

        writer.close();
    }
}
