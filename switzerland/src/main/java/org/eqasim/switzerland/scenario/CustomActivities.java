package org.eqasim.switzerland.scenario;

import org.matsim.core.config.CommandLine;

import java.util.Arrays;
import java.util.List;

public class CustomActivities {
    protected static Boolean hasCustomActivities = false;
    protected static List<String> activityTypes;

    static public void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("activity-list") //
                .build();

        hasCustomActivities = true;
        activityTypes = Arrays.asList(cmd.getOption("activity-list").get().split("\\s*,\\s*"));

    }

}
