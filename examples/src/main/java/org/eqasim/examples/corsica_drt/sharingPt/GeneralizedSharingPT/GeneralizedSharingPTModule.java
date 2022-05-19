package org.eqasim.examples.corsica_drt.sharingPt.GeneralizedSharingPT;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.RoutingModule;

public class GeneralizedSharingPTModule extends AbstractDvrpModeModule {
    Scenario scenario;
    private Controler controller;
    String name;

    public GeneralizedSharingPTModule(Scenario scenario, String name){
        super(name);
        this.scenario = scenario;
       
    }
    @Override
    public void install() {

        String modePTName=super.getMode()+"_PT";
        String pTmodeName="PT_"+super.getMode();
        String modePTModeName=super.getMode()+"_PT_"+super.getMode();
        bindModal(GeneralizedSharingPTRoutingModule.class).toProvider(modalProvider(getter -> {
            Scenario scenario = getter.get(Scenario.class);
            RoutingModule accessEgressRoutingModule = getter.getNamed(RoutingModule.class, "sharing:"+ super.getMode());
            RoutingModule mainModeRoutingModule = getter.getNamed(RoutingModule.class, TransportMode.pt);

            return new GeneralizedSharingPTRoutingModule(accessEgressRoutingModule,mainModeRoutingModule,scenario.getNetwork(),scenario.getTransitSchedule().getFacilities());
        }));

        addRoutingModuleBinding(modePTName).to(modalKey(GeneralizedSharingPTRoutingModule.class));
       // addRoutingModuleBinding("Sharing_PT").to(SharingPTRoutingModule.class);
        // Binds the  routing module for PT+ sharing egress

        bindModal(GeneralizedPTSharingRoutingModule.class).toProvider(modalProvider(getter -> {
            Scenario scenario = getter.get(Scenario.class);
            RoutingModule accessEgressRoutingModule = getter.getNamed(RoutingModule.class, "sharing:"+ super.getMode());
            RoutingModule mainModeRoutingModule = getter.getNamed(RoutingModule.class, TransportMode.pt);

            return new GeneralizedPTSharingRoutingModule(accessEgressRoutingModule,mainModeRoutingModule,scenario.getNetwork(),scenario.getTransitSchedule().getFacilities());
        }));
        addRoutingModuleBinding(pTmodeName).to(modalKey(GeneralizedPTSharingRoutingModule.class));

        bindModal(GeneralizedSharingPTSharingRoutingModule.class).toProvider(modalProvider(getter -> {
            Scenario scenario = getter.get(Scenario.class);
            RoutingModule accessEgressRoutingModule = getter.getNamed(RoutingModule.class,"sharing:"+ super.getMode());
            RoutingModule mainModeRoutingModule = getter.getNamed(RoutingModule.class, TransportMode.pt);

            return new GeneralizedSharingPTSharingRoutingModule(accessEgressRoutingModule,mainModeRoutingModule,scenario.getNetwork(),scenario.getTransitSchedule().getFacilities());
        }));


        addRoutingModuleBinding(modePTModeName).to(modalKey(GeneralizedSharingPTSharingRoutingModule.class));
    }

//    @Provides
//    public SharingPTRoutingModule provideSharingPTRoutingModule(@Named("pt") RoutingModule ptRoutingModule, @Named("sharing:bikeShare")RoutingModule sharingRoutingModule, Network network){
//
//
//        return new SharingPTRoutingModule(sharingRoutingModule,ptRoutingModule, network,scenario.getTransitSchedule().getFacilities());
//    }

//    @Provides
//    public SharingPTSharingRoutingModule provideSharingPTSharingRoutingModule(@Named("pt") RoutingModule ptRoutingModule, @Named("sharing:bikeShare")RoutingModule sharingRoutingModule,Network network){
//
//        return new SharingPTSharingRoutingModule(sharingRoutingModule,ptRoutingModule, network,scenario.getTransitSchedule().getFacilities());
//    }
//    @Provides
//    public PTSharingRoutingModule providePTSharingRoutingModule(@Named("pt") RoutingModule ptRoutingModule, @Named("sharing:bikeShare")RoutingModule sharingRoutingModule, Network network){
//        return new PTSharingRoutingModule(sharingRoutingModule,ptRoutingModule, network,scenario.getTransitSchedule().getFacilities());
//    }
//    @Provides
//    public PTStationFinder provideStationFinder(Scenario scenario) {
//
//        return new PTStationFinder(scenario.getTransitSchedule().getFacilities());
//
//    }
}
