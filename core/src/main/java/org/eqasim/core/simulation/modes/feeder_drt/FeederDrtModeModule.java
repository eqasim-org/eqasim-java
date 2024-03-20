package org.eqasim.core.simulation.modes.feeder_drt;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import org.eqasim.core.simulation.modes.feeder_drt.config.FeederDrtConfigGroup;
import org.eqasim.core.simulation.modes.feeder_drt.router.FeederDrtRoutingModule;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.modal.ModalAnnotationCreator;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.Map;


public class FeederDrtModeModule extends AbstractDvrpModeModule {

	private final FeederDrtConfigGroup config;

	public FeederDrtModeModule(FeederDrtConfigGroup feederDrtConfigGroup) {
		super(feederDrtConfigGroup.mode);
		this.config = feederDrtConfigGroup;
	}


	@Override
	public void install() {
		FeederDrtConfigGroup feederDrtConfigGroup = this.config;
		addRoutingModuleBinding(this.config.mode).toProvider(new Provider<>() {
			@Inject
			private Map<String, Provider<RoutingModule>> routingModuleProviders;

			@Inject
			private Injector injector;

			@Inject
			private Population population;

			@Inject
			private TransitSchedule transitSchedule;

			@Override
			public RoutingModule get() {
				RoutingModule ptRoutingModule = routingModuleProviders.get(feederDrtConfigGroup.ptModeName).get();
				RoutingModule drtRoutingModule = routingModuleProviders.get(feederDrtConfigGroup.accessEgressModeName).get();
				ModalAnnotationCreator<DvrpMode> modalAnnotationCreator = DvrpModes::mode;
				Provider<Network> networkProvider = injector.getProvider(modalAnnotationCreator.key(Network.class, feederDrtConfigGroup.accessEgressModeName));
				return new FeederDrtRoutingModule(feederDrtConfigGroup.mode, drtRoutingModule, ptRoutingModule, population.getFactory(), transitSchedule, networkProvider.get());
			}
		}).asEagerSingleton();
	}
}
