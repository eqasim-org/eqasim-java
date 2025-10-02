package org.eqasim.core.components.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.core.analysis.DistanceUnit;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class EqasimConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "eqasim";

	private final static String SAMPLE_SIZE = "sampleSize";
	private final static String DISTANCE_UNIT = "distanceUnit";

	private final static String CROSSING_PENALTY = "crossingPenalty";

	private final static String MODE_PARAMETERS_PATH = "modeParametersPath";
	private final static String COST_PARAMETERS_PATH = "costParametersPath";

	private final static String ANALYSIS_INTERVAL = "analysisInterval";
	private final static String ANALYSIS_DISTANCE_UNIT = "analysisDistanceUnit";
	
	private final static String TRAVEL_TIME_RECORDING_INTERVAL = "travelTimeRecordingInterval";

	private final static String USE_SCHEDULE_BASED_TRANSPORT = "useScheduleBasedTransport";

	private final static String USE_PSEUDO_RANDOM_ERRORS = "usePseudoRandomErrors";

	private final static String ADDITIONAL_AVAILABLE_MODES = "additionalAvailableModes";

	private double sampleSize = 1.0;
	private DistanceUnit distanceUnit = DistanceUnit.meter;

	private double crossingPenalty = 3.0;

	private String modeParametersPath = null;
	private String costParametersPath = null;

	private int analysisInterval = 0;
	private DistanceUnit analysisDistanceUnit = DistanceUnit.meter;
	
	private int travelTimeRecordingInterval = 0;

	private boolean useScheduleBasedTransport = true;

	private boolean usePseudoRandomErrors = false;

	private final Set<String> additionalAvailableModes = new HashSet<>();

	public EqasimConfigGroup() {
		super(GROUP_NAME);
	}
	
	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(SAMPLE_SIZE,
				"The sample size of the population you are simulating. This is normally set by the synthesis pipeline.");
		
		return map;
	}

	@StringGetter(CROSSING_PENALTY)
	public double getCrossingPenalty() {
		return crossingPenalty;
	}

	@StringSetter(CROSSING_PENALTY)
	public void setCrossingPenalty(double crossingPenalty) {
		this.crossingPenalty = crossingPenalty;
	}

	@StringGetter(SAMPLE_SIZE)
	public double getSampleSize() {
		return sampleSize;
	}

	@StringSetter(SAMPLE_SIZE)
	public void setSampleSize(double sampleSize) {
		this.sampleSize = sampleSize;
	}

	@StringGetter(USE_PSEUDO_RANDOM_ERRORS)
	public boolean getUsePseudoRandomErrors() {
		return usePseudoRandomErrors;
	}

	@StringSetter(USE_PSEUDO_RANDOM_ERRORS)
	public void setUsePseudoRandomErrors(boolean usePseudoRandomErrors) {
		this.usePseudoRandomErrors = usePseudoRandomErrors;
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		switch (type) {
		case EstimatorParameterSet.GROUP_NAME:
			return new EstimatorParameterSet();
		case CostModelParameterSet.GROUP_NAME:
			return new CostModelParameterSet();
		default:
			throw new IllegalArgumentException("Unknown parameter set type: " + type);
		}
	}

	private Optional<EstimatorParameterSet> getEstimatorParameterSet(String mode) {
		EstimatorParameterSet result = null;

		for (ConfigGroup group : getParameterSets(EstimatorParameterSet.GROUP_NAME)) {
			EstimatorParameterSet candidate = (EstimatorParameterSet) group;

			if (candidate.getMode().contentEquals(mode)) {
				result = candidate;
			}
		}

		return Optional.ofNullable(result);
	}

	public void setEstimator(String mode, String estimator) {
		Optional<EstimatorParameterSet> set = getEstimatorParameterSet(mode);

		if (set.isPresent()) {
			set.get().setEstimator(estimator);
		} else {
			EstimatorParameterSet newSet = new EstimatorParameterSet();

			newSet.setMode(mode);
			newSet.setEstimator(estimator);

			addParameterSet(newSet);
		}
	}

	public void removeEstimator(String mode) {
		Optional<EstimatorParameterSet> set = getEstimatorParameterSet(mode);

		if (set.isPresent()) {
			removeParameterSet(set.get());
		}
	}

	public Map<String, String> getEstimators() {
		Map<String, String> map = new HashMap<>();

		for (ConfigGroup group : getParameterSets(EstimatorParameterSet.GROUP_NAME)) {
			EstimatorParameterSet estimator = (EstimatorParameterSet) group;
			map.put(estimator.getMode(), estimator.getEstimator());
		}

		return map;
	}

	private Optional<CostModelParameterSet> getCostModelParameterSet(String mode) {
		CostModelParameterSet result = null;

		for (ConfigGroup group : getParameterSets(CostModelParameterSet.GROUP_NAME)) {
			CostModelParameterSet candidate = (CostModelParameterSet) group;

			if (candidate.getMode().contentEquals(mode)) {
				result = candidate;
			}
		}

		return Optional.ofNullable(result);
	}

	public void setCostModel(String mode, String model) {
		Optional<CostModelParameterSet> set = getCostModelParameterSet(mode);

		if (set.isPresent()) {
			set.get().setModel(model);
		} else {
			CostModelParameterSet newSet = new CostModelParameterSet();

			newSet.setMode(mode);
			newSet.setModel(model);

			addParameterSet(newSet);
		}
	}

	public void removeCostModel(String mode) {
		Optional<CostModelParameterSet> set = getCostModelParameterSet(mode);

		if (set.isPresent()) {
			removeParameterSet(set.get());
		}
	}

	public Map<String, String> getCostModels() {
		Map<String, String> map = new HashMap<>();

		for (ConfigGroup group : getParameterSets(CostModelParameterSet.GROUP_NAME)) {
			CostModelParameterSet estimator = (CostModelParameterSet) group;
			map.put(estimator.getMode(), estimator.getModel());
		}

		return map;
	}

	static public EqasimConfigGroup get(Config config) {
		if (!config.getModules().containsKey(GROUP_NAME)) {
			config.addModule(new EqasimConfigGroup());
		}

		return (EqasimConfigGroup) config.getModules().get(GROUP_NAME);
	}

	@StringGetter(MODE_PARAMETERS_PATH)
	public String getModeParametersPath() {
		return modeParametersPath;
	}

	@StringSetter(MODE_PARAMETERS_PATH)
	public void setModeParametersPath(String modeParametersPath) {
		this.modeParametersPath = modeParametersPath;
	}

	@StringGetter(COST_PARAMETERS_PATH)
	public String getCostParametersPath() {
		return costParametersPath;
	}

	@StringSetter(COST_PARAMETERS_PATH)
	public void setCostParametersPath(String costParametersPath) {
		this.costParametersPath = costParametersPath;
	}

	@StringGetter(ANALYSIS_INTERVAL)
	public int getAnalysisInterval() {
		return analysisInterval;
	}

	@StringSetter(ANALYSIS_INTERVAL)
	public void setAnalysisInterval(int analysisInterval) {
		this.analysisInterval = analysisInterval;
	}

	@StringGetter(TRAVEL_TIME_RECORDING_INTERVAL)
	public int getTravelTimeRecordingInterval() {
		return travelTimeRecordingInterval;
	}

	@StringSetter(TRAVEL_TIME_RECORDING_INTERVAL)
	public void setTravelTimeRecordingInterval(int travelTimeRecordingInterval) {
		this.travelTimeRecordingInterval = travelTimeRecordingInterval;
	}

	@StringGetter(DISTANCE_UNIT)
	public DistanceUnit getDistanceUnit() {
		return distanceUnit;
	}

	@StringSetter(DISTANCE_UNIT)
	public void setDistanceUnit(DistanceUnit distanceUnit) {
		this.distanceUnit = distanceUnit;
	}

	@StringGetter(ANALYSIS_DISTANCE_UNIT)
	public DistanceUnit getAnalysisDistanceUnit() {
		return analysisDistanceUnit;
	}

	@StringSetter(ANALYSIS_DISTANCE_UNIT)
	public void setAnalysisDistanceUnit(DistanceUnit analysisDistanceUnit) {
		this.analysisDistanceUnit = analysisDistanceUnit;
	}

	@StringGetter(USE_SCHEDULE_BASED_TRANSPORT)
	public boolean getUseScheduleBasedTransport() {
		return useScheduleBasedTransport;
	}

	@StringSetter(USE_SCHEDULE_BASED_TRANSPORT)
	public void setUseScheduleBasedTransport(boolean useScheduleBasedTransport) {
		this.useScheduleBasedTransport = useScheduleBasedTransport;
	}

	@StringSetter(ADDITIONAL_AVAILABLE_MODES)
	public void setAdditionalAvailableModesAsString(String val) {
		additionalAvailableModes.clear();

		for (var item : val.split(",")) {
			if (item.trim().length() > 0) {
				additionalAvailableModes.add(item.trim());
			}
		}
	}

	@StringGetter(ADDITIONAL_AVAILABLE_MODES)
	public String getAdditionalAvailableModesAsString() {
		return additionalAvailableModes.stream().collect(Collectors.joining(", "));
	}

	public Set<String> getAdditionalAvailableModes() {
		return Collections.unmodifiableSet(additionalAvailableModes);
	}

	public void setAdditionalAvailableModes(Set<String> val) {
		additionalAvailableModes.clear();
		additionalAvailableModes.addAll(val);
	}
}
