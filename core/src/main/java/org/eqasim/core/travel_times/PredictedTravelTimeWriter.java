package org.eqasim.core.travel_times;

import org.eqasim.core.travel_times.items.TripTravelTimeItem;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class PredictedTravelTimeWriter {

    private static BufferedWriter writer;
    private static Counter counter;

    public static void initialize(String outputPath) {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

            writer.write(TripTravelTimeItem.formatHeader() + "\n");
            writer.flush();

            counter = new Counter("Traverse #");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(TripTravelTimeItem tripTravelTimeItem) {

        try {
            writer.write(tripTravelTimeItem.formatItem() + "\n");
            writer.flush();
            counter.incCounter();

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            writer.flush();
            writer.close();
            counter.printCounter();
            System.out.println("Done");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
