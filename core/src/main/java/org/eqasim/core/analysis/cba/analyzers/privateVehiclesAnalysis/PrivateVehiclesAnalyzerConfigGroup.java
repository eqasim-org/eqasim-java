package org.eqasim.core.analysis.cba.analyzers.privateVehiclesAnalysis;

import jakarta.validation.constraints.NotNull;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PrivateVehiclesAnalyzerConfigGroup extends ReflectiveConfigGroup {

    public static final String SET_NAME = "privateVehiclesAnalyzer";

    public static final String MODES = "modes";
    public static final String MODES_EXP = "A comma separated list of the The names of modes that will be analyzed";

    public static final String TRIPS_SHEET_NAME = "tripsSheetName";
    public static final String TRIPS_SHEET_NAME_EXP = "The name of the sheet that will contain the trips information";

    public static final String IGNORED_ACTIVITY_TYPES = "ignoredActivityTypes";
    public static final String IGNORED_ACTIVITY_TYPES_EXP = "A comma separated list of activity types to ignore. If an activity type is ignored, the trips leading to or following activities of that type wil be ignored";

    @NotNull
    private String modesString;
    @NotNull
    private String tripsSheetName;
    private String ignoredActivityTypesString = "";

    private List<String> modes;

    private List<String> ignoredActivityTypes;

    public PrivateVehiclesAnalyzerConfigGroup() {
        super(SET_NAME);
    }

    /**
     * @param modesString -- {@value MODES_EXP}
     */
    @StringSetter(MODES)
    public void setMode(String modesString){
        this.modesString = modesString;
        this.modes = List.of(this.modesString.split(","));
    }

    /**
     * @return -- {@value MODES_EXP}
     */
    @StringGetter(MODES)
    public String getModesString(){
        return this.modesString;
    }

    public List<String> getModes() {
        return new ArrayList<>(this.modes);
    }

    /**
     * @param tripsSheetName -- {@value TRIPS_SHEET_NAME_EXP}
     */
    @StringSetter(TRIPS_SHEET_NAME)
    public void setTripsSheetName(String tripsSheetName){
        this.tripsSheetName = tripsSheetName;
    }

    /**
     * @return -- {@value TRIPS_SHEET_NAME_EXP}
     */
    @StringGetter(TRIPS_SHEET_NAME)
    public String getTripsSheetName() {
        return this.tripsSheetName;
    }

    /**
     * @return -- {@value IGNORED_ACTIVITY_TYPES_EXP}
     */
    @StringGetter(IGNORED_ACTIVITY_TYPES)
    public String getIgnoredActivityTypesString() {
        return ignoredActivityTypesString;
    }

    /**
     * @param ignoredActivityTypesString -- {@value IGNORED_ACTIVITY_TYPES_EXP}
     */
    @StringSetter(IGNORED_ACTIVITY_TYPES)
    public void setIgnoredActivityTypes(String ignoredActivityTypesString) {
        this.ignoredActivityTypesString = ignoredActivityTypesString;
        this.ignoredActivityTypes = List.of(this.ignoredActivityTypesString.split(","));
    }

    public List<String> getIgnoredActivityTypes() {
        return new ArrayList<>(this.ignoredActivityTypes);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(MODES, MODES_EXP);
        comments.put(TRIPS_SHEET_NAME, TRIPS_SHEET_NAME_EXP);
        comments.put(IGNORED_ACTIVITY_TYPES, IGNORED_ACTIVITY_TYPES_EXP);
        return comments;
    }
}
