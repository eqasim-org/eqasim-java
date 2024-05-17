package org.eqasim.core.analysis.cba;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eqasim.core.analysis.cba.analyzers.agentsAnalysis.AgentsAnalysisModule;
import org.eqasim.core.analysis.cba.analyzers.agentsAnalysis.AgentsAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.drtAnalysis.DrtAnalysisModule;
import org.eqasim.core.analysis.cba.analyzers.drtAnalysis.DrtAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.genericAnalysis.GenericAnalysisModule;
import org.eqasim.core.analysis.cba.analyzers.genericAnalysis.GenericAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.privateVehiclesAnalysis.PrivateVehiclesAnalysisModule;
import org.eqasim.core.analysis.cba.analyzers.privateVehiclesAnalysis.PrivateVehiclesAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.ptAnalysis.PtAnalysisModule;
import org.eqasim.core.analysis.cba.analyzers.ptAnalysis.PtAnalyzerConfigGroup;
import org.eqasim.core.analysis.cba.analyzers.rideAnalysis.RideAnalysisModule;
import org.eqasim.core.analysis.cba.analyzers.rideAnalysis.RideAnalyzerConfigGroup;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.MatsimServices;


public class CbaModule extends AbstractModule {

    @Override
    public void install() {

        CbaConfigGroup configGroup = (CbaConfigGroup) getConfig().getModules().get(CbaConfigGroup.GROUP_NAME);

        bind(CbaAnalysis.class).toProvider(new Provider<CbaAnalysis>() {
            @Inject
            private MatsimServices matsimServices;
            @Inject
            private Network network;
            @Override
            public CbaAnalysis get() {
                return new CbaAnalysis(configGroup, matsimServices, network);
            }
        }).asEagerSingleton();

        addControlerListenerBinding().toProvider(new Provider<CbaControlerListener>() {
            @Inject
            private CbaAnalysis cbaAnalysis;
            @Inject
            private Network network;
            @Inject
            private MatsimServices matsimServices;
            @Override
            public CbaControlerListener get() {
                return new CbaControlerListener(cbaAnalysis, matsimServices, network, null, null);
            }
        });

        for(DrtAnalyzerConfigGroup drtAnalyzerConfigGroup : configGroup.getDrtAnalyzersConfigs()) {
            install(new DrtAnalysisModule(drtAnalyzerConfigGroup, configGroup));
        }

        for(PtAnalyzerConfigGroup ptAnalyzerConfigGroup: configGroup.getPtAnalyzersConfigs()) {
            install(new PtAnalysisModule(ptAnalyzerConfigGroup, configGroup));
        }

        for(AgentsAnalyzerConfigGroup agentsAnalyzerConfigGroup : configGroup.getAgentsAnalyzersConfigs()) {
            install(new AgentsAnalysisModule(agentsAnalyzerConfigGroup));
        }

        for(PrivateVehiclesAnalyzerConfigGroup privateVehiclesAnalyzerConfigGroup : configGroup.getPrivateVehiclesAnalyzersConfigs()) {
            install(new PrivateVehiclesAnalysisModule(privateVehiclesAnalyzerConfigGroup, configGroup));
        }

        for(GenericAnalyzerConfigGroup genericAnalyzerConfigGroup : configGroup.getWalkAnalyzersConfigs()) {
            install(new GenericAnalysisModule(genericAnalyzerConfigGroup, configGroup));
        }

        for(RideAnalyzerConfigGroup rideAnalyzerConfigGroup : configGroup.getRideAnalyzersConfigs()) {
            install(new RideAnalysisModule(rideAnalyzerConfigGroup, configGroup));
        }
    }
}
