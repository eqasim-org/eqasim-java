package org.eqasim.core.standalone_mode_choice;

import jakarta.validation.constraints.NotNull;
import org.matsim.core.config.ReflectiveConfigGroup;

public class StandaloneModeChoiceConfigGroup extends ReflectiveConfigGroup {

    public static final String GROUP_NAME = "standaloneModeChoice";
    public static final String REMOVE_PERSON_WITH_NO_VALID_ALTERNATIVES = "removePersonsWithNoValidAlternatives";
    public static final String OUTPUT_DIRECTORY = "outputDirectory";
    public static final String CHUNK_SIZE = "chunkSize";

    private boolean removePersonsWithNoValidAlternative = false;
    private int chunkSize = 1000;

    @NotNull
    private String outputDirectory = "output_mode_choice";

    public StandaloneModeChoiceConfigGroup() {
        super(GROUP_NAME);
    }

    @StringGetter(REMOVE_PERSON_WITH_NO_VALID_ALTERNATIVES)
    public boolean isRemovePersonsWithNoValidAlternative() {
        return this.removePersonsWithNoValidAlternative;
    }

    @StringSetter(REMOVE_PERSON_WITH_NO_VALID_ALTERNATIVES)
    public void setRemovePersonsWithNoValidAlternative(boolean removePersonsWithNoValidAlternative) {
        this.removePersonsWithNoValidAlternative = removePersonsWithNoValidAlternative;
    }

    @StringGetter(OUTPUT_DIRECTORY)
    public String getOutputDirectory() {
        return this.outputDirectory;
    }

    @StringSetter(OUTPUT_DIRECTORY)
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    @StringGetter(CHUNK_SIZE)
    public int getChunkSize() {
        return this.chunkSize;
    }

    @StringSetter(CHUNK_SIZE)
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
}
