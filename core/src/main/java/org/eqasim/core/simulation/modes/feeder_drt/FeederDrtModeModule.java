package org.eqasim.core.simulation.modes.feeder_drt;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.simulation.modes.feeder_drt.config.AccessEgressStopSelectorParams;
import org.eqasim.core.simulation.modes.feeder_drt.config.FeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.router.FeederDrtRoutingModule;
import org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_selector.AccessEgressStopsSelector;
import org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_selector.ClosestAccessEgressStopSelector;
import org.eqasim.core.simulation.modes.feeder_drt.router.access_egress_selector.ClosestAccessEgressStopSelectorParameterSet;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.modal.ModalAnnotationCreator;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;


public class FeederDrtModeModule extends AbstractDvrpModeModule {

	private final FeederDrtConfigGroup config;

	public FeederDrtModeModule(FeederDrtConfigGroup feederDrtConfigGroup) {
		super(feederDrtConfigGroup.mode);
		this.config = feederDrtConfigGroup;
	}


	@Override
	public void install() {
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) getConfig().getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
		DrtConfigGroup coveredDrtConfig = multiModeDrtConfigGroup.getModalElements().stream().filter(drtConfigGroup -> drtConfigGroup.getMode().equals(this.config.accessEgressModeName)).findFirst().get();

		ScenarioExtent serviceAreaExtent = null;
		if(coveredDrtConfig.operationalScheme.equals(DrtConfigGroup.OperationalScheme.serviceAreaBased)) {
			URI extentPath;
			try {
				extentPath = ConfigGroup.getInputFileURL(getConfig().getContext(), coveredDrtConfig.drtServiceAreaShapeFile).toURI();
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
			try {
				serviceAreaExtent = new ShapeScenarioExtent.Builder(new File(extentPath), Optional.empty(), Optional.empty()).build();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		ScenarioExtent finalServiceAreaExtent = serviceAreaExtent;

		FeederDrtConfigGroup feederDrtConfigGroup = this.config;
		ReflectiveConfigGroup reflectiveConfigGroup = feederDrtConfigGroup.getAccessEgressStopSelectorConfig().getAccessEgressStopSelectorParams();
		if(reflectiveConfigGroup instanceof ClosestAccessEgressStopSelectorParameterSet closestAccessEgressStopSelectorConfig) {
			bindModal(AccessEgressStopsSelector.class).toProvider(new Provider<>() {
				@Inject
				private Injector injector;

				@Inject
				private TransitSchedule transitSchedule;

				@Override
				public AccessEgressStopsSelector get() {
					ModalAnnotationCreator<DvrpMode> modalAnnotationCreator = DvrpModes::mode;
					Provider<Network> networkProvider = injector.getProvider(modalAnnotationCreator.key(Network.class, feederDrtConfigGroup.accessEgressModeName));
					return new ClosestAccessEgressStopSelector(closestAccessEgressStopSelectorConfig, networkProvider.get(), transitSchedule, finalServiceAreaExtent);
				}
			}).asEagerSingleton();
		} else {
			throw new RuntimeException(String.format("Unsupported %s: %s", AccessEgressStopSelectorParams.NAME, reflectiveConfigGroup));
		}

		addRoutingModuleBinding(this.config.mode).toProvider(new Provider<>() {
			@Inject
			private Map<String, Provider<RoutingModule>> routingModuleProviders;

			@Inject
			private Injector injector;

			@Inject
			private Population population;

			@Override
			public RoutingModule get() {
				RoutingModule ptRoutingModule = routingModuleProviders.get(feederDrtConfigGroup.ptModeName).get();
				RoutingModule drtRoutingModule = routingModuleProviders.get(feederDrtConfigGroup.accessEgressModeName).get();
				ModalAnnotationCreator<DvrpMode> modalAnnotationCreator = DvrpModes::mode;
				Provider<AccessEgressStopsSelector> accessEgressStopsSelectorProvider = injector.getProvider(modalAnnotationCreator.key(AccessEgressStopsSelector.class, feederDrtConfigGroup.mode));
				return new FeederDrtRoutingModule(feederDrtConfigGroup.mode, drtRoutingModule, ptRoutingModule, population.getFactory(), accessEgressStopsSelectorProvider.get(), finalServiceAreaExtent);
			}
		});
	}
}
