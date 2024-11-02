package org.eqasim.core.simulation.vdf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.vdf.handlers.VDFHorizonHandler;
import org.eqasim.core.simulation.vdf.handlers.VDFInterpolationHandler;
import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.eqasim.core.simulation.vdf.travel_time.VDFTravelTime;
import org.eqasim.core.simulation.vdf.travel_time.function.BPRFunction;
import org.eqasim.core.simulation.vdf.travel_time.function.VolumeDelayFunction;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFModule extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
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
			VolumeDelayFunction vdf, QSimConfigGroup qsimConfig, EqasimConfigGroup eqasimConfig) throws IOException {
		ScenarioExtent updateExtent = config.getUpdateAreaShapefile() == null ? null
				: new ShapeScenarioExtent.Builder(new File(ConfigGroup
						.getInputFileURL(getConfig().getContext(), config.getUpdateAreaShapefile()).getPath()),
						Optional.empty(), Optional.empty()).build();
		return new VDFTravelTime(scope, config.getMinimumSpeed(), config.getCapacityFactor(),
				eqasimConfig.getSampleSize(), network, vdf, eqasimConfig.getCrossingPenalty(), updateExtent);
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
