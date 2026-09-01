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
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.DefaultNetworkRouteCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.DefaultNetworkTripCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkRouteCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkTripCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing.LinkTimingRegistry;
import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.DefaultTeleportationCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.DefaultTransitRouteCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.DefaultTransitTripCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitRouteCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitTripCrossingPointFinder;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;

import com.google.inject.Provider;
import com.google.inject.Provides;

public class PopulationCutterModule extends AbstractModule {
	private final int numberOfThreads;
	private final int batchSize;
	private final ScenarioExtent extent;
	private final LinkTimingRegistry linkTimingRegistry;

	public PopulationCutterModule(ScenarioExtent extent, int numberOfThreads, int batchSize,
			LinkTimingRegistry linkTimingRegistry) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.extent = extent;
		this.linkTimingRegistry = linkTimingRegistry;
	}

	@Override
	public void install() {
		bind(TripProcessor.class).to(ModeAwareTripProcessor.class);

		bind(TeleportationCrossingPointFinder.class).to(DefaultTeleportationCrossingPointFinder.class);
		bind(NetworkRouteCrossingPointFinder.class).to(DefaultNetworkRouteCrossingPointFinder.class);
		bind(TransitRouteCrossingPointFinder.class).to(DefaultTransitRouteCrossingPointFinder.class);
		bind(TransitTripCrossingPointFinder.class).to(DefaultTransitTripCrossingPointFinder.class);
		bind(NetworkTripCrossingPointFinder.class).to(DefaultNetworkTripCrossingPointFinder.class);

		bind(MergeOutsideActivities.class).to(DefaultMergeOutsideActivities.class);

		bind(TeleportationTripProcessor.class);

		bind(PlanCutter.class);

		bind(ScenarioExtent.class).toInstance(extent);
		bind(LinkTimingRegistry.class).toInstance(linkTimingRegistry);
	}

	@Provides
	PopulationCutter providePopulationCutter(Provider<PlanCutter> planCutterProvider, Population population) {
		return new PopulationCutter(planCutterProvider, population.getFactory(), numberOfThreads, batchSize);
	}

	private Collection<String> getTeleportedModes(RoutingConfigGroup routingConfig) {
		Collection<String> teleportedModes = new HashSet<>();

		for (Map.Entry<String, TeleportedModeParams> entry : routingConfig.getTeleportedModeParams().entrySet()) {
			String mode = entry.getKey();
			TeleportedModeParams params = entry.getValue();

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
	public ModeAwareTripProcessor provideModeAwareTripProcessor(RoutingConfigGroup routingConfig,
			TransitConfigGroup transitConfig, ScenarioExtent extent,
			TeleportationTripProcessor teleportationTripProcessor, NetworkTripProcessor networkTripProcessor,
			TransitTripProcessor transitTripProcessor) {
		ModeAwareTripProcessor tripProcessor = new ModeAwareTripProcessor();

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

	@Provides
	public NetworkTripProcessor provideNetworkTripProcessor(NetworkTripCrossingPointFinder networkPointFinder,
			ScenarioExtent extent, TransitRouterConfigGroup routerConfig) {
		return new NetworkTripProcessor(networkPointFinder, extent);
	}

}
