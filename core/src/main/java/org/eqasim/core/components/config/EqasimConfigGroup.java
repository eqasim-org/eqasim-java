package org.eqasim.core.components.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

	private final static String USE_SCHEDULE_BASED_TRANSPORT = "useScheduleBasedTransport";

	private double sampleSize = 1.0;
	private DistanceUnit distanceUnit = DistanceUnit.meter;

	private double crossingPenalty = 3.0;

	private String modeParametersPath = null;
	private String costParametersPath = null;

	private int analysisInterval = 0;
	private DistanceUnit analysisDistanceUnit = DistanceUnit.meter;

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
		case ModeParameterSet.GROUP_NAME:
			return new ModeParameterSet();
		case CostModelParameterSet.GROUP_NAME:
			return new CostModelParameterSet();
		default:
			throw new IllegalArgumentException("Unknown parameter set type: " + type);
		}
	}

	public Optional<ModeParameterSet> getMode(String mode) {
		ModeParameterSet result = null;

		for (ModeParameterSet candidate : getModes()) {
			if (candidate.getMode().contentEquals(mode)) {
				result = candidate;
				break;
			}
		}

		return Optional.ofNullable(result);
	}

	public ModeParameterSet addMode(String mode) {
		Optional<ModeParameterSet> existingSet = getMode(mode);
		
		if (existingSet.isPresent()) {
			return existingSet.get();
		}

		ModeParameterSet newSet = new ModeParameterSet();
		newSet.setMode(mode);
		addParameterSet(newSet);
		return newSet;
	}

	public void removeMode(String mode) {
		Optional<ModeParameterSet> set = getMode(mode);

		if (set.isPresent()) {
			removeParameterSet(set.get());
		}
	}

	public Collection<ModeParameterSet> getModes() {
		return getParameterSets(ModeParameterSet.GROUP_NAME).stream().map(ModeParameterSet.class::cast)
				.collect(Collectors.toUnmodifiableList());
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
}
