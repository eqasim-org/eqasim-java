package org.eqasim.switzerland.ch;

import java.util.HashMap;
import java.util.Map;

import org.eqasim.switzerland.ch.utils.pt.PTLinkVolumesAnalyser;
import org.eqasim.switzerland.ch.utils.pt.PTLinkVolumesHandler;
import org.eqasim.switzerland.ch.utils.pt.TransitTripInfo;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class PTLinkVolumesModule extends AbstractModule{

	@SuppressWarnings("deprecation")
    @Override
	public void install() {
		addControlerListenerBinding().to(PTLinkVolumesAnalyser.class);
		
	}
	
	@Provides
	@Singleton
	public PTLinkVolumesHandler providePTLinkVolumesHandler(Scenario scenario) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		Map<Id<Vehicle>, TransitTripInfo> vehicleToTripInfo = new HashMap<>();

        for (TransitLine line : schedule.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                for (Departure dep : route.getDepartures().values()) {
                    Id<Vehicle> vehicleId = dep.getVehicleId();
                    if (vehicleId != null) {
                        vehicleToTripInfo.put(vehicleId, new TransitTripInfo(
                            line.getId().toString(),
                            line.getName().toString(),
                            route.getId().toString(),
                            dep.getId().toString()
                        ));
                    }
                }
            }
        }

        return new PTLinkVolumesHandler(vehicleToTripInfo);
	}

}
