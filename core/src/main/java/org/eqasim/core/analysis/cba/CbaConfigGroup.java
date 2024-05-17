package org.eqasim.core.analysis.cba;

import org.eqasim.core.analysis.cba.analyzers.agentsAnalysis.AgentsAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.drtAnalysis.DrtAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.genericAnalysis.GenericAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.privateVehiclesAnalysis.PrivateVehiclesAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.ptAnalysis.PtAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.rideAnalysis.RideAnalyzerConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Collection;
import java.util.Map;


public class CbaConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "cba";
    public static final String OUTPUT_FREQUENCY = "outputFrequency";
    public static final String OUTPUT_FREQUENCY_EXP = "The frequency at which cba output are generated";

    private int outputFrequency = 1;

    /**
     * @param outputFrequency -- {@value OUTPUT_FREQUENCY_EXP}
     */
    @StringSetter(OUTPUT_FREQUENCY)
    public void setOutputFrequency(int outputFrequency) {
        this.outputFrequency = outputFrequency;
    }

    /**
     * @return -- {@value OUTPUT_FREQUENCY_EXP}
     */
    @StringGetter(OUTPUT_FREQUENCY)
    public int getOutputFrequency(){
        return this.outputFrequency;
    }

    @Override
    public ConfigGroup createParameterSet(String type) {
        switch (type) {
            case DrtAnalyzerConfigGroup.SET_NAME:
                return new DrtAnalyzerConfigGroup();
            case PtAnalyzerConfigGroup.SET_NAME:
                return new PtAnalyzerConfigGroup();
            case AgentsAnalyzerConfigGroup.SET_NAME:
                return new AgentsAnalyzerConfigGroup();
            case PrivateVehiclesAnalyzerConfigGroup.SET_NAME:
                return new PrivateVehiclesAnalyzerConfigGroup();
            case GenericAnalyzerConfigGroup.SET_NAME:
                return new GenericAnalyzerConfigGroup();
            case RideAnalyzerConfigGroup.SET_NAME:
                return new RideAnalyzerConfigGroup();
            default:
                throw new IllegalArgumentException("Unsupported parameter set type: " + type);
        }
    }

    @Override
    public void addParameterSet(ConfigGroup set) {
        if(set instanceof DrtAnalyzerConfigGroup || set instanceof PtAnalyzerConfigGroup || set instanceof AgentsAnalyzerConfigGroup || set instanceof PrivateVehiclesAnalyzerConfigGroup || set instanceof GenericAnalyzerConfigGroup || set instanceof RideAnalyzerConfigGroup) {
            super.addParameterSet(set);
        } else {
            throw new IllegalArgumentException("Unsupported parameter set class: " + set);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<DrtAnalyzerConfigGroup> getDrtAnalyzersConfigs() {
        return (Collection<DrtAnalyzerConfigGroup>) getParameterSets(DrtAnalyzerConfigGroup.SET_NAME);
    }

    @SuppressWarnings("unchecked")
    public Collection<PtAnalyzerConfigGroup> getPtAnalyzersConfigs() {
        return (Collection<PtAnalyzerConfigGroup>) getParameterSets(PtAnalyzerConfigGroup.SET_NAME);
    }

    @SuppressWarnings("unchecked")
    public Collection<AgentsAnalyzerConfigGroup> getAgentsAnalyzersConfigs() {
        return (Collection<AgentsAnalyzerConfigGroup>) getParameterSets(AgentsAnalyzerConfigGroup.SET_NAME);
    }

    @SuppressWarnings("unchecked")
    public Collection<PrivateVehiclesAnalyzerConfigGroup> getPrivateVehiclesAnalyzersConfigs() {
        return (Collection<PrivateVehiclesAnalyzerConfigGroup>) getParameterSets(PrivateVehiclesAnalyzerConfigGroup.SET_NAME);
    }

    @SuppressWarnings("unchecked")
    public Collection<GenericAnalyzerConfigGroup> getWalkAnalyzersConfigs() {
        return (Collection<GenericAnalyzerConfigGroup>) getParameterSets(GenericAnalyzerConfigGroup.SET_NAME);
    }

    @SuppressWarnings("unchecked")
    public Collection<RideAnalyzerConfigGroup> getRideAnalyzersConfigs() {
        return (Collection<RideAnalyzerConfigGroup>) getParameterSets(RideAnalyzerConfigGroup.SET_NAME);
    }

    public CbaConfigGroup() {
        super(GROUP_NAME);
    }

    public static CbaConfigGroup get(Config config) {
        return (CbaConfigGroup)config.getModule(GROUP_NAME);
    }

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(OUTPUT_FREQUENCY, OUTPUT_FREQUENCY_EXP);
        return comments;
    }
}
