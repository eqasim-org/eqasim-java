package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedMultimodalRoutingSMM;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.RoutingModule;

/**
 * Class creates abstract module that permits the creation of routing modules of PT-SMM modes
 */
public class SMMSharingPTModule extends AbstractDvrpModeModule {
    Scenario scenario;
    private Controler controller;
    String name;

    public SMMSharingPTModule(Scenario scenario, String name){
        super(name);
        this.scenario = scenario;
       
    }

    /**
     * Method install the routing modules for SMM-PT; SMM-PT-SMM and PT-SMM modes
     */
    @Override
    public void install() {

        String modePTName=super.getMode()+"_PT";
        String pTmodeName="PT_"+super.getMode();
        String modePTModeName=super.getMode()+"_PT_"+super.getMode();
        //Binds a SMM-PT routing module to a  a provider with access using SMM and main mode PT
        bindModal(SMMSharingPTRoutingModule.class).toProvider(modalProvider(getter -> {
            Scenario scenario = getter.get(Scenario.class);
            RoutingModule accessEgressRoutingModule = getter.getNamed(RoutingModule.class, "sharing:"+ super.getMode());
            RoutingModule mainModeRoutingModule = getter.getNamed(RoutingModule.class, TransportMode.pt);

            return new SMMSharingPTRoutingModule(accessEgressRoutingModule,mainModeRoutingModule,scenario.getNetwork(),scenario.getTransitSchedule().getFacilities());
        }));
        // Binds the name of the SMM-PT mode to the router
        addRoutingModuleBinding(modePTName).to(modalKey(SMMSharingPTRoutingModule.class));

        //Binds a PT-SMM routing module to a  a provider with egress using SMM and main mode PT
        bindModal(SMMPTSharingRoutingModule.class).toProvider(modalProvider(getter -> {
            Scenario scenario = getter.get(Scenario.class);
            RoutingModule accessEgressRoutingModule = getter.getNamed(RoutingModule.class, "sharing:"+ super.getMode());
            RoutingModule mainModeRoutingModule = getter.getNamed(RoutingModule.class, TransportMode.pt);

            return new SMMPTSharingRoutingModule(accessEgressRoutingModule,mainModeRoutingModule,scenario.getNetwork(),scenario.getTransitSchedule().getFacilities());
        }));
        // Binds the name of the PT-SMM mode to the router
        addRoutingModuleBinding(pTmodeName).to(modalKey(SMMPTSharingRoutingModule.class));
        //Binds a SMM-PT-SMM routing module to  a provider with egress using SMM and main mode PT
        bindModal(SMMSharingPTSharingRoutingModule.class).toProvider(modalProvider(getter -> {
            Scenario scenario = getter.get(Scenario.class);
            RoutingModule accessEgressRoutingModule = getter.getNamed(RoutingModule.class,"sharing:"+ super.getMode());
            RoutingModule mainModeRoutingModule = getter.getNamed(RoutingModule.class, TransportMode.pt);

            return new SMMSharingPTSharingRoutingModule(accessEgressRoutingModule,mainModeRoutingModule,scenario.getNetwork(),scenario.getTransitSchedule().getFacilities());
        }));
        // Binds the name of the SMM-PT-SMM mode to the router
        addRoutingModuleBinding(modePTModeName).to(modalKey(SMMSharingPTSharingRoutingModule.class));
    }
}
