package org.eqasim.examples.corsica_drt.sharingPt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

public class PTStopsManager extends AbstractModule {

   private static Scenario scenario;

    public PTStopsManager(Scenario scenario){
       this.scenario=scenario;
    }


//        @Provides
//    public static Map<Id<TransitStopFacility>,TransitStopFacility> getStopsFacility(){
//            TransitSchedule tSchedule=scenario.getTransitSchedule();
//        return (tempStF);
//    }
    @Override
    public void install() {

    }
}
