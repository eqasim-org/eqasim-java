package org.eqasim.core.analysis.sociodemographics;

import org.matsim.households.Household;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

public class HouseholdsWriter {
    final private HouseholdInfo householdInfo;
    final private String delimiter;

    public HouseholdsWriter(HouseholdInfo householdInfo) {
        this(householdInfo, ";");
    }

    public HouseholdsWriter(HouseholdInfo householdInfo, String delimiter) {
        this.householdInfo = householdInfo;
        this.delimiter = delimiter;
    }

    public void write(String outputPath) throws IOException{
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (Household household : householdInfo.households) {
            writer.write(formatItem(household) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        List<String> strings = new LinkedList<>();

        // add person id
        strings.add("household_id");

        // add all other attributes
        strings.addAll(householdInfo.householdAttributes);

        return String.join(delimiter, strings);
    }

    private String formatItem(Household household) {

        List<String> strings = new LinkedList<>();

        // add person id
        strings.add(household.getId().toString());

        // add all remaining attributes
        // we use the getOrDefault since some attributes are not present, and we default to null
        // we then get the string value of so that the null values are correctly converted to string
        // and do not return null pointer exceptions
        for (String attribute : householdInfo.householdAttributes) {
            strings.add(String.valueOf(household.getAttributes().getAsMap().getOrDefault(attribute, null)));
        }

        return String.join(delimiter, strings);
    }

}