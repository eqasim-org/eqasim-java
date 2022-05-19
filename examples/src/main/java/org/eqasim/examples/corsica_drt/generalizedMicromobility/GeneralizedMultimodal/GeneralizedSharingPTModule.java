package org.eqasim.examples.corsica_drt.generalizedMicromobility.GeneralizedMultimodal;

import com.google.inject.Provides;
import com.google.inject.name.Named;
import org.eqasim.examples.corsica_drt.sharingPt.PTSharingRoutingModule;
import org.eqasim.examples.corsica_drt.sharingPt.PTStationFinder;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTRoutingModule;
import org.eqasim.examples.corsica_drt.sharingPt.SharingPTSharingRoutingModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.router.RoutingModule;

public class GeneralizedSharingPTModule extends AbstractDvrpModeModule {
    Scenario scenario;

    String name;
    public GeneralizedSharingPTModule(Scenario scenario, String name){
        super(name);
        this.scenario = scenario;
        this.name=name;
    }
    @Override
    public void install() {
        addRoutingModuleBinding("Sharing_PT").to(SharingPTRoutingModule.class);
        addRoutingModuleBinding("PT_Sharing").to(PTSharingRoutingModule.class);
        addRoutingModuleBinding("Sharing_PT_Sharing").to(SharingPTSharingRoutingModule.class);
    }
    @Provides
    public SharingPTRoutingModule provideSharingPTRoutingModule(@Named("pt") RoutingModule ptRoutingModule, @Named("sharing:bikeShare")RoutingModule sharingRoutingModule, Network network){
        return new SharingPTRoutingModule(sharingRoutingModule,ptRoutingModule, network,scenario.getTransitSchedule().getFacilities());
    }

    @Provides
    public SharingPTSharingRoutingModule provideSharingPTSharingRoutingModule(@Named("pt") RoutingModule ptRoutingModule, @Named("sharing:bikeShare")RoutingModule sharingRoutingModule, Network network){
        return new SharingPTSharingRoutingModule(sharingRoutingModule,ptRoutingModule, network,scenario.getTransitSchedule().getFacilities());
    }
    @Provides
    public PTSharingRoutingModule providePTSharingRoutingModule(@Named("pt") RoutingModule ptRoutingModule, @Named("sharing:bikeShare")RoutingModule sharingRoutingModule, Network network){
        return new PTSharingRoutingModule(sharingRoutingModule,ptRoutingModule, network,scenario.getTransitSchedule().getFacilities());
    }
    @Provides
    public PTStationFinder provideStationFinder(Scenario scenario) {

        return new PTStationFinder(scenario.getTransitSchedule().getFacilities());

    }
}
