package org.eqasim.core.simulation.vdf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.flow.FlowDataSet;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.vdf.handlers.*;
import org.eqasim.core.simulation.vdf.travel_time.VDFTravelTime;
import org.eqasim.core.simulation.vdf.travel_time.function.BPRFunction;
import org.eqasim.core.simulation.vdf.travel_time.function.VolumeDelayFunction;
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

		// for (String mode : vdfConfig.getModes()) {
		//	LOGGER.info("Adding VDF travel time for mode " + mode);
		//	addTravelTimeBinding(mode).to(VDFTravelTime.class);
		//}

		addControllerListenerBinding().to(VDFUpdateListener.class);
		bind(VolumeDelayFunction.class).to(BPRFunction.class);

		switch (vdfConfig.getHandler()) {
			case Horizon:
				bind(VDFTrafficHandler.class).to(VDFHorizonHandler.class);
				addEventHandlerBinding().to(VDFHorizonHandler.class);
				break;
			case SparseHorizon:
				bind(VDFTrafficHandler.class).to(VDFSparseHorizonHandler.class);
				addEventHandlerBinding().to(VDFSparseHorizonHandler.class);
				break;
			case Interpolation:
				bind(VDFTrafficHandler.class).to(VDFInterpolationHandler.class);
				addEventHandlerBinding().to(VDFInterpolationHandler.class);
				break;
			case LowMemory:
				DelaysConfigGroup delaysConfig = DelaysConfigGroup.getOrCreate(getConfig());
				// impose consistency between the flow calculation and the VDF calculation, otherwise the flow data will not be compatible for the VDF handler
				boolean isConsistent = (delaysConfig.getStartTime()==vdfConfig.getStartTime() &&
										delaysConfig.getEndTime()==vdfConfig.getEndTime() &&
										delaysConfig.getBinSize()==vdfConfig.getInterval());
				if (!isConsistent) {
					LOGGER.warn("The flow calculation and the VDF calculation are not consistent. This may lead to incompatible flow data for the VDF handler.");
					vdfConfig.setStartTime(delaysConfig.getStartTime());
					vdfConfig.setEndTime(delaysConfig.getEndTime());
					vdfConfig.setInterval(delaysConfig.getBinSize());
				}

				addEventHandlerBinding().to(LinkFlowCounter.class).asEagerSingleton();
				addControllerListenerBinding().to(LinkFlowCounter.class).asEagerSingleton();
				bind(VDFTrafficHandler.class).to(LowMemoryHandler.class);
				break;
			default:
				throw new IllegalStateException();
		}
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
				controllerConfig.getFirstIteration(), inputFile);
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
	public LowMemoryHandler provideLowMemoryHandler(Network network, VDFScope scope,
													LinkFlowCounter linkFlowCounter, FlowDataSet flowDataSet) {
		return new LowMemoryHandler(network, scope, linkFlowCounter, flowDataSet);
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
