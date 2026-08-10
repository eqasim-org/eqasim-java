package org.eqasim.switzerland.ch_cmdp;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.eqasim.switzerland.ch_cmdp.utils.pt.PTIntermodalTripAnalyser;
import org.eqasim.switzerland.ch_cmdp.utils.pt.PTIntermodalTripHandler;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vehicles.Vehicle;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class PTIntermodalTripsModule extends AbstractModule {
	@Override
	public void install() {
		SwissRailRaptorConfigGroup raptorConfig = ConfigUtils.addOrGetModule(getConfig(),
				SwissRailRaptorConfigGroup.class);

		if (raptorConfig.isUseIntermodalAccessEgress()) {
			addControlerListenerBinding().to(PTIntermodalTripAnalyser.class);
		}
	}

	@Provides
	@Singleton
	public PTIntermodalTripHandler providePTIntermodalTripHandler(Scenario scenario) {
		SwissRailRaptorConfigGroup raptorConfig = ConfigUtils.addOrGetModule(scenario.getConfig(),
				SwissRailRaptorConfigGroup.class);
		SwissIntermodalAccessEgressConfigGroup intermodalConfig = SwissIntermodalAccessEgressConfigGroup
				.getOrCreate(scenario.getConfig());
		Set<String> intermodalModes = new LinkedHashSet<>();
		raptorConfig.getIntermodalAccessEgressParameterSets().forEach(parameters -> {
			if (parameters.getMode() != null) {
				intermodalModes.add(parameters.getMode());
			}
		});

		if (intermodalModes.isEmpty()) {
			intermodalModes.addAll(intermodalConfig.getRestrictedIntermodalAccessEgressModes());
		}

		intermodalModes.remove(TransportMode.walk);
		intermodalModes.remove(TransportMode.pt);

		Set<org.matsim.api.core.v01.Id<Vehicle>> transitVehicleIds = new LinkedHashSet<>();
		scenario.getTransitSchedule().getTransitLines().values().forEach(line -> line.getRoutes().values()
				.forEach(route -> route.getDepartures().values().forEach(departure -> {
					if (departure.getVehicleId() != null) {
						transitVehicleIds.add(departure.getVehicleId());
					}
				})));

		return new PTIntermodalTripHandler(scenario.getNetwork(), scenario.getTransitSchedule(), intermodalModes,
				transitVehicleIds);
	}
}
