package org.eqasim.core.analysis.cba.analyzers.ptAnalysis;

import jakarta.validation.constraints.NotNull;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class PtAnalyzerConfigGroup extends ReflectiveConfigGroup {

    public static final String SET_NAME = "ptAnalyzer";

    public static final String MODE = "mode";
    public static final String MODE_EXP = "The name of the pt mode that will be analyzed";

    public static final String TRIPS_SHEET_NAME = "tripsSheetName";
    public static final String TRIPS_SHEET_NAME_EXP = "The name of the sheet that will contain the trips information";

    public static final String VEHICLES_SHEET_NAME = "vehiclesSheetName";
    public static final String VEHICLES_SHEET_NAME_EXP = "The name of the sheet that will contain vehicles information";

    @NotNull
    private String mode;
    @NotNull
    private String tripsSheetName;
    private String vehiclesSheetName;

    public PtAnalyzerConfigGroup() {
        super(SET_NAME);
    }

    /**
     * @param mode -- {@value MODE_EXP}
     */
    @StringSetter(MODE)
    public void setMode(String mode){
        this.mode = mode;
    }

    /**
     * @return -- {@value MODE_EXP}
     */
    @StringGetter(MODE)
    public String getMode(){
        return this.mode;
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
     * @param vehiclesSheetName -- {@value VEHICLES_SHEET_NAME_EXP}
     */
    @StringSetter(VEHICLES_SHEET_NAME)
    public void setVehiclesSheetName(String vehiclesSheetName) {
        this.vehiclesSheetName = vehiclesSheetName;
    }

    /**
     * @return -- {@value VEHICLES_SHEET_NAME_EXP}
     */
    @StringGetter(VEHICLES_SHEET_NAME)
    public String getVehiclesSheetName() {
        return this.vehiclesSheetName;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(MODE, MODE_EXP);
        comments.put(TRIPS_SHEET_NAME, TRIPS_SHEET_NAME_EXP);
        comments.put(VEHICLES_SHEET_NAME, VEHICLES_SHEET_NAME_EXP);
        return comments;
    }
}
