package org.eqasim.core.scenario.cutter.population;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.ModeAwareTripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.NetworkTripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.TeleportationTripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.TransitTripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.TripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.DefaultNetworkCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.DefaultTeleportationCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.DefaultTransitRouteCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.DefaultTransitTripCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitRouteCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitTripCrossingPointFinder;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;

import com.google.inject.Provider;
import com.google.inject.Provides;

public class PopulationCutterModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;
	private final ScenarioExtent extent;

	public PopulationCutterModule(ScenarioExtent extent, int numberOfThreads, int batchSize) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.extent = extent;
	}

	@Override
	public void install() {
		bind(TripProcessor.class).to(ModeAwareTripProcessor.class);

		bind(TeleportationCrossingPointFinder.class).to(DefaultTeleportationCrossingPointFinder.class);
		bind(NetworkCrossingPointFinder.class).to(DefaultNetworkCrossingPointFinder.class);
		bind(TransitRouteCrossingPointFinder.class).to(DefaultTransitRouteCrossingPointFinder.class);
		bind(TransitTripCrossingPointFinder.class).to(DefaultTransitTripCrossingPointFinder.class);

		bind(MergeOutsideActivities.class).to(DefaultMergeOutsideActivities.class);

		bind(TeleportationTripProcessor.class);
		bind(NetworkTripProcessor.class);

		bind(PlanCutter.class);

		bind(ScenarioExtent.class).toInstance(extent);
	}

	@Provides
	PopulationCutter providePopulationCutter(Provider<PlanCutter> planCutterProvider, Population population) {
		return new PopulationCutter(planCutterProvider, population.getFactory(), numberOfThreads, batchSize);
	}

	private Collection<String> getTeleportedModes(PlansCalcRouteConfigGroup routingConfig) {
		Collection<String> teleportedModes = new HashSet<>();

		for (Map.Entry<String, ModeRoutingParams> entry : routingConfig.getModeRoutingParams().entrySet()) {
			String mode = entry.getKey();
			ModeRoutingParams params = entry.getValue();

			if (params.getTeleportedModeFreespeedFactor() != null) {
				throw new IllegalStateException("Cutter does not support teleportedModeFreespeedFactor yet!");
			}

			if (params.getTeleportedModeSpeed() != null) {
				teleportedModes.add(mode);
			}
		}

		return teleportedModes;
	}

	private boolean checkDisjoint(Collection<String> a, Collection<String> b) {
		for (String element : a) {
			if (b.contains(element)) {
				return false;
			}
		}

		for (String element : b) {
			if (a.contains(element)) {
				return false;
			}
		}

		return true;
	}

	@Provides
	public ModeAwareTripProcessor provideModeAwareTripProcessor(PlansCalcRouteConfigGroup routingConfig,
			TransitConfigGroup transitConfig, ScenarioExtent extent, MainModeIdentifier mainModeIdentifier,
			TeleportationTripProcessor teleportationTripProcessor, NetworkTripProcessor networkTripProcessor,
			TransitTripProcessor transitTripProcessor) {
		ModeAwareTripProcessor tripProcessor = new ModeAwareTripProcessor(mainModeIdentifier);

		Collection<String> networkModes = new HashSet<>(routingConfig.getNetworkModes());
		Collection<String> teleportedModes = getTeleportedModes(routingConfig);

		if (!checkDisjoint(networkModes, teleportedModes)) {
			throw new IllegalStateException("Network modes and teleported modes are not disjoint");
		}

		for (String networkMode : networkModes) {
			tripProcessor.setProcessor(networkMode, networkTripProcessor);
		}

		for (String teleportedMode : teleportedModes) {
			tripProcessor.setProcessor(teleportedMode, teleportationTripProcessor);
		}

		if (transitConfig.isUseTransit()) {
			// TODO: This may not only be "pt"!
			tripProcessor.setProcessor(TransportMode.pt, transitTripProcessor);
		}

		return tripProcessor;
	}

	@Provides
	public TransitTripProcessor provideTransitTripProcessor(TransitTripCrossingPointFinder transitPointFinder,
			ScenarioExtent extent, TransitRouterConfigGroup routerConfig) {
		return new TransitTripProcessor(transitPointFinder, extent, routerConfig.getAdditionalTransferTime());
	}
}
