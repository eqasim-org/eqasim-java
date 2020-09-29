package org.eqasim.core.components.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

	private final static String TRIP_ANALYSIS_INTERVAL = "tripAnalysisInterval";
	private final static String TRIP_ANALYSIS_DISTANCE_UNIT = "tripAnalysisDistanceUnit";

	private final static String USE_SCHEDULE_BASED_TRANSPORT = "useScheduleBasedTransport";

	private double sampleSize = 1.0;
	private DistanceUnit distanceUnit = DistanceUnit.meter;

	private double crossingPenalty = 3.0;

	private String modeParametersPath = null;
	private String costParametersPath = null;

	private int tripAnalysisInterval = 0;
	private DistanceUnit tripAnalysisDistanceUnit = DistanceUnit.meter;

	private boolean useScheduleBasedTransport = true;

	public EqasimConfigGroup() {
		super(GROUP_NAME);
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

	@StringGetter(TRIP_ANALYSIS_INTERVAL)
	public int getTripAnalysisInterval() {
		return tripAnalysisInterval;
	}

	@StringSetter(TRIP_ANALYSIS_INTERVAL)
	public void setTripAnalysisInterval(int tripAnalysisInterval) {
		this.tripAnalysisInterval = tripAnalysisInterval;
	}

	@StringGetter(DISTANCE_UNIT)
	public DistanceUnit getDistanceUnit() {
		return distanceUnit;
	}

	@StringSetter(DISTANCE_UNIT)
	public void setDistanceUnit(DistanceUnit distanceUnit) {
		this.distanceUnit = distanceUnit;
	}

	@StringGetter(TRIP_ANALYSIS_DISTANCE_UNIT)
	public DistanceUnit getTripAnalysisDistanceUnit() {
		return tripAnalysisDistanceUnit;
	}

	@StringSetter(TRIP_ANALYSIS_DISTANCE_UNIT)
	public void setTripAnalysisDistanceUnit(DistanceUnit tripAnalysisDistanceUnit) {
		this.tripAnalysisDistanceUnit = tripAnalysisDistanceUnit;
	}

	@StringGetter(USE_SCHEDULE_BASED_TRANSPORT)
	public boolean getUseScheduleBasedTransport() {
		return useScheduleBasedTransport;
	}

	@StringSetter(USE_SCHEDULE_BASED_TRANSPORT)
	public void setUseScheduleBasedTransport(boolean useScheduleBasedTransport) {
		this.useScheduleBasedTransport = useScheduleBasedTransport;
	}
}
