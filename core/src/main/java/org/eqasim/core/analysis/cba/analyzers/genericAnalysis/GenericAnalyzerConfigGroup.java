package org.eqasim.core.analysis.cba.analyzers.genericAnalysis;

import jakarta.validation.constraints.NotNull;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class GenericAnalyzerConfigGroup extends ReflectiveConfigGroup {

    public static final String SET_NAME = "genericAnalyzer";

    public static final String TRIPS_SHEET_NAME = "tripsSheetName";
    public static final String TRIPS_SHEET_NAME_EXP = "The name of the sheet that will contain the trips information";

    public static final String MODE = "mode";
    public static final String MODE_EXP = "The mode that will be analyzed by the generic analyzer";

    @NotNull
    private String tripsSheetName;

    @NotNull
    private String mode;

    public GenericAnalyzerConfigGroup() {
        super(SET_NAME);
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
     * @param mode -- {@value MODE_EXP}
     */
    @StringSetter(MODE)
    public void setMode(String mode) {
        this.mode = mode;
    }

    /**
     * @return -- {@value TRIPS_SHEET_NAME_EXP}
     */
    @StringGetter(MODE)
    public String getMode() {
        return this.mode;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(TRIPS_SHEET_NAME, TRIPS_SHEET_NAME_EXP);
        return comments;
    }
}
