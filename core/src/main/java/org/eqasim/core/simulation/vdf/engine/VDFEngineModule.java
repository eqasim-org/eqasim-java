package org.eqasim.core.simulation.vdf.engine;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.vdf.FlowEquivalentProvider;
import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.eqasim.core.simulation.vdf.travel_time.VDFTravelTime;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFEngineModule extends AbstractEqasimExtension {
	public static final String COMPONENT_NAME = "VDFEngine";

	@Override
	public void installEqasimExtension() {
		VDFEngineConfigGroup engineConfig = VDFEngineConfigGroup.getOrCreate(getConfig());

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding(COMPONENT_NAME).to(VDFEngine.class);
			}

			@Provides
			@Singleton
			public VDFEngine provideVDFEngine(VDFTravelTime travelTime, Network network, VDFTrafficHandler handler,
					QSimConfigGroup qsimConfig, FlowEquivalentProvider flowEquivalentProvider) {
				boolean generateNetworkEvents = engineConfig.getGenerateNetworkEventsInterval() > 0
						&& (getIterationNumber() % engineConfig.getGenerateNetworkEventsInterval() == 0);

				return new VDFEngine(engineConfig.getModes(), engineConfig.getDynamicModes(),
						new TraversalTime(network, travelTime, qsimConfig.getTimeStepSize()), flowEquivalentProvider,
						network,
						handler, generateNetworkEvents, qsimConfig.getTimeStepSize());
			}
		});
	}

	static public void configureQSim(QSimComponentsConfig components) {
		components.addNamedComponent(COMPONENT_NAME);
	}
}
