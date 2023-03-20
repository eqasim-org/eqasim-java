package org.eqasim.vdf;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.vdf.function.BPRFunction;
import org.eqasim.vdf.function.VolumeDelayFunction;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFModule extends AbstractModule {
	@Override
	public void install() {
		VDFConfigGroup vdfConfig = VDFConfigGroup.getOrCreate(getConfig());

		for (String mode : vdfConfig.getModes()) {
			addTravelTimeBinding(mode).to(VDFTravelTime.class);
		}

		addEventHandlerBinding().to(VDFTrafficHandler.class);
		bind(VolumeDelayFunction.class).to(BPRFunction.class);
	}

	@Provides
	@Singleton
	public VDFTravelTime provideVDFTravelTime(VDFConfigGroup config, Network network, VolumeDelayFunction vdf,
			QSimConfigGroup qsimConfig, EqasimConfigGroup eqasimConfig) {
		int numberOfIntervals = (int) Math.floor((config.getEndTime() - config.getStartTime()) / config.getInterval())
				+ 1;
		return new VDFTravelTime(config.getEndTime(), config.getInterval(), numberOfIntervals, config.getMinimumSpeed(),
				qsimConfig.getFlowCapFactor(), eqasimConfig.getSampleSize(), network, vdf, eqasimConfig.getCrossingPenalty());
	}

	@Provides
	@Singleton
	public VDFTrafficHandler provideVDFTrafficHandler(VDFConfigGroup config, Network network,
			VDFTravelTime travelTime) {
		return new VDFTrafficHandler(network, travelTime, config.getHorizon());
	}

	@Provides
	@Singleton
	public BPRFunction provideBPRFunction(VDFConfigGroup config) {
		return new BPRFunction(config.getBprFactor(), config.getBprExponent());
	}
}
