package org.eqasim.core.analysis.cba.analyzers.rideAnalysis;

import org.eqasim.core.analysis.cba.CbaAnalysis;
import org.eqasim.core.analysis.cba.CbaConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import javax.inject.Inject;
import javax.inject.Provider;

public class RideAnalysisModule extends AbstractModule {

    private final CbaConfigGroup cbaConfigGroup;
    private final RideAnalyzerConfigGroup configGroup;

    public RideAnalysisModule(RideAnalyzerConfigGroup configGroup, CbaConfigGroup cbaConfigGroup) {
        this.configGroup = configGroup;
        this.cbaConfigGroup = cbaConfigGroup;
    }
    @Override
    public void install() {
        installQSimModule(new AbstractQSimModule() {
            @Override
            protected void configureQSim() {
                if(this.getIterationNumber() % cbaConfigGroup.getOutputFrequency() != 0) {
                    return;
                }
                addMobsimScopeEventHandlerBinding().toProvider(new Provider<MobsimScopeEventHandler>() {
                    @Inject
                    CbaAnalysis cbaAnalysis;

                    @Override
                    public MobsimScopeEventHandler get() {
                        RideAnalyzer rideAnalyzer = new RideAnalyzer(configGroup);
                        cbaAnalysis.addSingleIterationAnalyzer(rideAnalyzer);
                        return rideAnalyzer;
                    }
                });
            }
        });
    }
}
