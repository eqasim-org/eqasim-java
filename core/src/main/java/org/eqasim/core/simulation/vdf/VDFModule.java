package org.eqasim.core.simulation.vdf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.core.simulation.vdf.handlers.VDFHorizonHandler;
import org.eqasim.core.simulation.vdf.handlers.VDFInterpolationHandler;
import org.eqasim.core.simulation.vdf.handlers.VDFSparseHorizonHandler;
import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.eqasim.core.simulation.vdf.travel_time.VDFTravelTime;
import org.eqasim.core.simulation.vdf.travel_time.function.BPRFunction;
import org.eqasim.core.simulation.vdf.travel_time.function.VolumeDelayFunction;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFModule extends AbstractEqasimExtension {

	private static final Logger LOGGER = LogManager.getLogger(VDFModule.class);

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
				break;
			case SparseHorizon:
				bind(VDFTrafficHandler.class).to(VDFSparseHorizonHandler.class);
				break;
			case Interpolation:
				bind(VDFTrafficHandler.class).to(VDFInterpolationHandler.class);
				break;
			default:
				throw new IllegalStateException();
		}

		if (getTrackedModes().size() > 0) {
			// no need to put a listener if no modes need to be tracked
			addEventHandlerBinding().to(VDFTrafficListener.class);
		}
	}

	private Set<String> getTrackedModes() {
		// we listen to all modes that are defined for VDF
		Set<String> trackedModes = new HashSet<>(VDFConfigGroup.getOrCreate(getConfig()).getModes());

		// except for those that are simulated by the vdf engine - their flows are
		// directly added to the handlers, no need to listen to events
		VDFEngineConfigGroup engineConfig = (VDFEngineConfigGroup) getConfig().getModules()
				.get(VDFEngineConfigGroup.GROUP_NAME);

		if (engineConfig != null) {
			trackedModes.removeAll(engineConfig.getModes());
		}

		return trackedModes;
	}

	@Provides
	@Singleton
	public FlowEquivalentProvider provideFlowEquivalentProvider(Scenario scenario) {
		return new FlowEquivalentProvider(scenario.getVehicles(), scenario.getTransitVehicles());
	}

	@Provides
	@Singleton
	public VDFTrafficListener provideVDFTrafficListener(VDFTrafficHandler handler,
			FlowEquivalentProvider flowEquivalentProvider) {
		return new VDFTrafficListener(handler, flowEquivalentProvider, getTrackedModes());
	}

	@Provides
	@Singleton
	public VDFUpdateListener provideVDFUpdateListener(VDFScope scope, VDFTrafficHandler handler,
			VDFTravelTime travelTime, VDFConfigGroup config, OutputDirectoryHierarchy outputHierarchy, Network network,
			ControllerConfigGroup controllerConfig) {
		URL inputFile = config.getInputFlowFile() == null ? null
				: ConfigGroup.getInputFileURL(getConfig().getContext(), config.getInputFlowFile());
		return new VDFUpdateListener(network, scope, handler, travelTime, outputHierarchy, config.getWriteInterval(),
				config.getWriteFlowInterval(), config.getWriteTravelTimesInterval(),
				controllerConfig.getFirstIteration(), inputFile, controllerConfig.getCompressionType());
	}

	@Provides
	@Singleton
	public VDFScope provideVDFScope(VDFConfigGroup config) {
		return new VDFScope(config.getStartTime(), config.getEndTime(), config.getInterval());
	}

	@Provides
	@Singleton
	public VDFTravelTime provideVDFTravelTime(VDFConfigGroup config, VDFScope scope, Network network,
			VolumeDelayFunction vdf, QSimConfigGroup qsimConfig, EqasimConfigGroup eqasimConfig,
			CrossingPenalty crossingPenalty) throws IOException {
		ScenarioExtent updateExtent = config.getUpdateAreaShapefile() == null ? null
				: new ShapeScenarioExtent.Builder(new File(ConfigGroup
						.getInputFileURL(getConfig().getContext(), config.getUpdateAreaShapefile()).getPath()),
						Optional.empty(), Optional.empty()).build();

		VDFTravelTime vdfTravelTime = new VDFTravelTime(scope, config.getMinimumSpeed(), config.getCapacityFactor(),
				eqasimConfig.getSampleSize(), network, vdf, crossingPenalty, updateExtent);
		if (config.getInputTravelTimesFile() != null) {
			LOGGER.info("Reading VDF travel times");
			URL inputTimeFile = ConfigGroup.getInputFileURL(getConfig().getContext(), config.getInputTravelTimesFile());
			vdfTravelTime.readFrom(inputTimeFile);
			LOGGER.info("  Done");
		}
		return vdfTravelTime;
	}

	@Provides
	@Singleton
	public VDFHorizonHandler provideVDFHorizonHandler(VDFConfigGroup config, Network network, VDFScope scope) {
		return new VDFHorizonHandler(network, scope, config.getHorizon(), getConfig().global().getNumberOfThreads());
	}

	@Provides
	@Singleton
	public VDFSparseHorizonHandler provideVDFSparseHorizonHandler(VDFConfigGroup config, Network network,
			VDFScope scope) {
		return new VDFSparseHorizonHandler(network, scope, config.getHorizon(),
				getConfig().global().getNumberOfThreads());
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
