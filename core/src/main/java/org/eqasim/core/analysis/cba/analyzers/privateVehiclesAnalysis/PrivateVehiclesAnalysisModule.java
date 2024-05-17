package org.eqasim.core.analysis.cba.analyzers.privateVehiclesAnalysis;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eqasim.core.analysis.cba.CbaAnalysis;
import org.eqasim.core.analysis.cba.CbaConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class PrivateVehiclesAnalysisModule extends AbstractModule {

    private final PrivateVehiclesAnalyzerConfigGroup configGroup;
    private final CbaConfigGroup cbaConfigGroup;

    public PrivateVehiclesAnalysisModule(PrivateVehiclesAnalyzerConfigGroup configGroup, CbaConfigGroup cbaConfigGroup) {
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

                    @Inject
                    Network network;

                    @Override
                    public MobsimScopeEventHandler get() {
                        PrivateVehiclesAnalyzer privateVehiclesAnalyzer = new PrivateVehiclesAnalyzer(configGroup, network);
                        cbaAnalysis.addSingleIterationAnalyzer(privateVehiclesAnalyzer);
                        return privateVehiclesAnalyzer;
                    }
                });
            }
        });
    }
}
