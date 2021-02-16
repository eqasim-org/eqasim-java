package org.eqasim.vdf;

import org.eqasim.vdf.function.BPRFunction;
import org.eqasim.vdf.function.VolumeDelayFunction;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFModule extends AbstractModule {
	private final VDFConfig config;

	public VDFModule(VDFConfig config) {
		this.config = config;
	}

	@Override
	public void install() {
		addTravelTimeBinding(TransportMode.car).to(VDFTravelTime.class);
		addEventHandlerBinding().to(VDFTrafficHandler.class);

		bind(VolumeDelayFunction.class).toInstance(new BPRFunction());
	}

	@Provides
	@Singleton
	public VDFTravelTime provideVDFTravelTime(Network network, VolumeDelayFunction vdf) {
		int numberOfIntervals = (int) Math.floor((config.endTime - config.startTime) / config.interval) + 1;
		return new VDFTravelTime(config.startTime, config.interval, numberOfIntervals, config.minimumSpeed, network,
				vdf);
	}

	@Provides
	@Singleton
	public VDFTrafficHandler provideVDFTrafficHandler(Network network, VDFTravelTime travelTime) {
		return new VDFTrafficHandler(network, travelTime);
	}
}
