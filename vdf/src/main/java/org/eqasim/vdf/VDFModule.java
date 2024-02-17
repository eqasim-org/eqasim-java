package org.eqasim.vdf;

import java.net.URL;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.vdf.handlers.VDFHorizonHandler;
import org.eqasim.vdf.handlers.VDFInterpolationHandler;
import org.eqasim.vdf.handlers.VDFTrafficHandler;
import org.eqasim.vdf.travel_time.VDFTravelTime;
import org.eqasim.vdf.travel_time.function.BPRFunction;
import org.eqasim.vdf.travel_time.function.VolumeDelayFunction;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFModule extends AbstractModule {
	@Override
	public void install() {
		VDFConfigGroup vdfConfig = VDFConfigGroup.getOrCreate(getConfig());

		for (String mode : vdfConfig.getModes()) {
			addTravelTimeBinding(mode).to(VDFTravelTime.class);
		}

		addControlerListenerBinding().to(VDFUpdateListener.class);
		bind(VolumeDelayFunction.class).to(BPRFunction.class);

		switch (vdfConfig.getHandler()) {
		case Horizon:
			bind(VDFTrafficHandler.class).to(VDFHorizonHandler.class);
			addEventHandlerBinding().to(VDFHorizonHandler.class);
			break;
		case Interpolation:
			bind(VDFTrafficHandler.class).to(VDFInterpolationHandler.class);
			addEventHandlerBinding().to(VDFInterpolationHandler.class);
			break;
		default:
			throw new IllegalStateException();
		}
	}

	@Provides
	@Singleton
	public VDFUpdateListener provideVDFUpdateListener(VDFScope scope, VDFTrafficHandler handler,
			VDFTravelTime travelTime, VDFConfigGroup config, OutputDirectoryHierarchy outputHierarchy,
			Network network) {
		URL inputFile = config.getInputFile() == null ? null
				: ConfigGroup.getInputFileURL(getConfig().getContext(), config.getInputFile());
		return new VDFUpdateListener(network, scope, handler, travelTime, outputHierarchy, config.getWriteInterval(),
				config.getWriteFlowInterval(), inputFile);
	}

	@Provides
	@Singleton
	public VDFScope provideVDFScope(VDFConfigGroup config) {
		return new VDFScope(config.getStartTime(), config.getEndTime(), config.getInterval());
	}

	@Provides
	@Singleton
	public VDFTravelTime provideVDFTravelTime(VDFConfigGroup config, VDFScope scope, Network network,
			VolumeDelayFunction vdf, QSimConfigGroup qsimConfig, EqasimConfigGroup eqasimConfig) {
		return new VDFTravelTime(scope, config.getMinimumSpeed(), config.getCapacityFactor(),
				eqasimConfig.getSampleSize(), network, vdf, eqasimConfig.getCrossingPenalty());
	}

	@Provides
	@Singleton
	public VDFHorizonHandler provideVDFHorizonHandler(VDFConfigGroup config, Network network, VDFScope scope) {
		return new VDFHorizonHandler(network, scope, config.getHorizon(), getConfig().global().getNumberOfThreads());
	}

	@Provides
	@Singleton
	public VDFInterpolationHandler provideVDFInterpolationHandler(VDFConfigGroup config, Network network,
			VDFScope scope) {
		return new VDFInterpolationHandler(network, scope, 1.0 / config.getHorizon());
	}

	@Provides
	@Singleton
	public BPRFunction provideBPRFunction(VDFConfigGroup config) {
		return new BPRFunction(config.getBprFactor(), config.getBprExponent());
	}
}
