package org.eqasim.switzerland.scenario;

import java.util.Arrays;
import java.util.List;

public class CustomActivities {
    protected static Boolean hasCustomActivities = false;
    protected static List<String> activityTypes;

    static public void run (String activityList) {
        hasCustomActivities = true;
        activityTypes = Arrays.asList(activityList.split("\\s*,\\s*"));
    }

}
