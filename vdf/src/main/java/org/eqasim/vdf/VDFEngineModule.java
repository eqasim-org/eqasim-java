package org.eqasim.vdf;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.common.base.Verify;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFEngineModule extends AbstractModule {
	public static final String COMPONENT_NAME = "VDFEngine";

	private final Collection<String> engineModes;

	public VDFEngineModule(Collection<String> engineModes) {
		this.engineModes = new ArrayList<>(engineModes);
	}

	@Override
	public void install() {
		for (String mode : engineModes) {
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
			public VDFEngine provideVDFEngine(VDFTravelTime travelTime, Network network) {
				return new VDFEngine(engineModes, travelTime, network);
			}
		});
	}
}
