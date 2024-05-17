package org.eqasim.core.analysis.cba.analyzers.agentsAnalysis;

import jakarta.validation.constraints.NotNull;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class AgentsAnalyzerConfigGroup extends ReflectiveConfigGroup {

    public static final String SET_NAME = "agentsAnalyzer";

    public static final String SCORES_SHEET_NAME = "scoresSheetName";
    public static final String SCORES_SHEET_NAME_EXP = "The name of the sheet where the agents' scores will be printed";

    @NotNull
    private String scoresSheetName;

    public AgentsAnalyzerConfigGroup() {
        super(SET_NAME);
    }

    /**
     * @param scoresSheetName -- {@value SCORES_SHEET_NAME_EXP}
     */
    @StringSetter(SCORES_SHEET_NAME)
    public void setScoresSheetName(String scoresSheetName){
        this.scoresSheetName = scoresSheetName;
    }

    /**
     * @return -- {@value SCORES_SHEET_NAME_EXP}
     */
    @StringGetter(SCORES_SHEET_NAME)
    public String getScoresSheetName() {
        return this.scoresSheetName;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(SCORES_SHEET_NAME, SCORES_SHEET_NAME_EXP);
        return comments;
    }
}
