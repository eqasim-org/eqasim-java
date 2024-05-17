package org.eqasim.core.analysis.cba.analyzers.rideAnalysis;

import org.matsim.core.config.ReflectiveConfigGroup;

public class RideAnalyzerConfigGroup extends ReflectiveConfigGroup {


    public static final String SET_NAME = "rideAnalyzer";

    public static final String TRIPS_SHEET_NAME = "tripsSheetName";
    public static final String TRIPS_SHEET_NAME_EXP = "Name of the sheet where ride trips will be written";


    private String tripsSheetName;

    public RideAnalyzerConfigGroup() {
        super(SET_NAME);
    }

    /**
     *
     * @param tripsSheetName -- {@value TRIPS_SHEET_NAME_EXP}
     */
    @StringSetter(TRIPS_SHEET_NAME)
    public void setTripsSheetName(String tripsSheetName) {
        this.tripsSheetName = tripsSheetName;
    }

    /**
     * @return -- {@value TRIPS_SHEET_NAME_EXP}
     */
    @StringGetter(TRIPS_SHEET_NAME)
    public String getTripsSheetName(){
        return this.tripsSheetName;
    }

}
