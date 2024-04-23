package org.eqasim.core.simulation.modes.transit_with_abstract_access.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

public class AbstractAccessLegWriter {
    private final Collection<AbstractAccessLegItem> abstractAccessLegs;
    private final String delimiter;


    public AbstractAccessLegWriter(Collection<AbstractAccessLegItem> abstractAccessLegs, String delimiter) {
        this.abstractAccessLegs = abstractAccessLegs;
        this.delimiter = delimiter;
    }

    public void write(String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();
        for(AbstractAccessLegItem abstractAccessLegItem: this.abstractAccessLegs) {
            writer.write(formatLeg(abstractAccessLegItem) + "\n");
            writer.flush();
        }
        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(this.delimiter, new String[]{
                "person_id",
                "person_trip_id",
                "leg_index",
                "transit_stop_id",
                "leaving_transit_stop",
                "is_distance_routed",
                "distance"
        });
    }

    private String formatLeg(AbstractAccessLegItem abstractAccessLeg) {
        return String.join(this.delimiter, new String[]{
                abstractAccessLeg.personId.toString(),
                String.valueOf(abstractAccessLeg.personTripId),
                String.valueOf(abstractAccessLeg.legIndex),
                abstractAccessLeg.transitStopFacilityId.toString(),
                String.valueOf(abstractAccessLeg.leavingCenterStop),
                String.valueOf(abstractAccessLeg.isRouted),
                String.valueOf(abstractAccessLeg.distance)
        });
    }
}
