package org.eqasim.vdf.engine;

import org.eqasim.vdf.handlers.VDFTrafficHandler;
import org.eqasim.vdf.travel_time.VDFTravelTime;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.common.base.Verify;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFEngineModule extends AbstractModule {
	public static final String COMPONENT_NAME = "VDFEngine";

	@Override
	public void install() {
		VDFEngineConfigGroup engineConfig = VDFEngineConfigGroup.getOrCreate(getConfig());

		for (String mode : engineConfig.getModes()) {
			Verify.verify(!getConfig().qsim().getMainModes().contains(mode));
			Verify.verify(!getConfig().plansCalcRoute().getModeRoutingParams().containsKey(mode));
		}

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding(COMPONENT_NAME).to(VDFEngine.class);
			}

			@Provides
			@Singleton
			public VDFEngine provideVDFEngine(VDFTravelTime travelTime, Network network, VDFTrafficHandler handler) {
				return new VDFEngine(engineConfig.getModes(), travelTime, network, handler,
						engineConfig.getGenerateNetworkEvents());
			}
		});
	}
}
