package org.eqasim.core.components.config;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class EqasimConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "eqasim";

	private final static String CROSSING_PENALTY = "crossingPenalty";
	private final static String SAMPLE_SIZE = "sampleSize";

	private double crossingPenalty = 3.0;
	private double sampleSize = 1.0;

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
	public ConfigGroup createParameterSet(String parameterSetType) {
		switch (parameterSetType) {
		case ModeUtilityMapping.GROUP_NAME:
			return new ModeUtilityMapping();
		case CostModelMapping.GROUP_NAME:
			return new CostModelMapping();
		default:
			throw new IllegalStateException("Unknown parameter set type: " + parameterSetType);
		}
	}

	public final Map<String, String> getModeUtilityMappings() {
		Map<String, String> mappings = new HashMap<>();

		for (ConfigGroup group : getParameterSets(ModeUtilityMapping.GROUP_NAME)) {
			ModeUtilityMapping mapping = (ModeUtilityMapping) group;
			mappings.put(mapping.getMode(), mapping.getEstimator());
		}

		return mappings;
	}

	public void removeModeUtilityMapping(String mode) {
		if (hasModeUtilityMapping(mode)) {
			ModeUtilityMapping item = null;

			for (ConfigGroup group : getParameterSets(ModeUtilityMapping.GROUP_NAME)) {
				ModeUtilityMapping mapping = (ModeUtilityMapping) group;

				if (mapping.getMode().equals(mode)) {
					item = mapping;
					break;
				}
			}

			removeParameterSet(item);
		} else {
			throw new IllegalStateException(String.format("Mode utility mapping for '%s' does not exist!", mode));
		}
	}

	public void addModeUtilityMapping(String mode, String estimator) {
		if (hasModeUtilityMapping(mode)) {
			throw new IllegalStateException(String.format("Mode utility mapping for '%s' already exists!", mode));
		} else {
			addParameterSet(new ModeUtilityMapping(mode, estimator));
		}
	}

	public boolean hasModeUtilityMapping(String mode) {
		for (ConfigGroup group : getParameterSets(ModeUtilityMapping.GROUP_NAME)) {
			ModeUtilityMapping mapping = (ModeUtilityMapping) group;

			if (mapping.getMode().equals(mode)) {
				return true;
			}
		}

		return false;
	}

	private class ModeUtilityMapping extends ReflectiveConfigGroup {
		static public final String GROUP_NAME = "mode_mapping";

		static public final String MODE = "mode";
		static public final String ESTIMATOR = "estimator";

		private String mode;
		private String estimator;

		public ModeUtilityMapping() {
			super(GROUP_NAME);
		}

		public ModeUtilityMapping(String mode, String estimator) {
			this();

			this.mode = mode;
			this.estimator = estimator;
		}

		@StringGetter(MODE)
		public String getMode() {
			return mode;
		}

		@StringSetter(MODE)
		public void setMode(String mode) {
			this.mode = mode;
		}

		@StringGetter(ESTIMATOR)
		public String getEstimator() {
			return estimator;
		}

		@StringSetter(ESTIMATOR)
		public void setEstimator(String estimator) {
			this.estimator = estimator;
		}
	}

	public final Map<String, String> getCostModelMappings() {
		Map<String, String> mappings = new HashMap<>();

		for (ConfigGroup group : getParameterSets(CostModelMapping.GROUP_NAME)) {
			CostModelMapping mapping = (CostModelMapping) group;
			mappings.put(mapping.getMode(), mapping.getModel());
		}

		return mappings;
	}

	public void removeCostModelMapping(String mode) {
		if (hasCostModelMapping(mode)) {
			CostModelMapping item = null;

			for (ConfigGroup group : getParameterSets(CostModelMapping.GROUP_NAME)) {
				CostModelMapping mapping = (CostModelMapping) group;

				if (mapping.getMode().equals(mode)) {
					item = mapping;
					break;
				}
			}

			removeParameterSet(item);
		} else {
			throw new IllegalStateException(String.format("Cost model mapping '%s' does not exist!", mode));
		}
	}

	public void addCostModelMapping(String mode, String model) {
		if (hasCostModelMapping(mode)) {
			throw new IllegalStateException(String.format("Cost model mapping for '%s' already exists!", mode));
		} else {
			addParameterSet(new CostModelMapping(mode, model));
		}
	}

	public boolean hasCostModelMapping(String mode) {
		for (ConfigGroup group : getParameterSets(CostModelMapping.GROUP_NAME)) {
			CostModelMapping mapping = (CostModelMapping) group;

			if (mapping.getMode().equals(mode)) {
				return true;
			}
		}

		return false;
	}

	private class CostModelMapping extends ReflectiveConfigGroup {
		static public final String GROUP_NAME = "cost_model_mappding";

		static public final String MODE = "mode";
		static public final String MODEL = "model";

		private String mode;
		private String model;

		public CostModelMapping() {
			super(GROUP_NAME);
		}

		public CostModelMapping(String mode, String model) {
			this();

			this.mode = mode;
			this.model = model;
		}

		@StringGetter(MODE)
		public String getMode() {
			return mode;
		}

		@StringSetter(MODE)
		public void setMode(String mode) {
			this.mode = mode;
		}

		@StringGetter(MODEL)
		public String getModel() {
			return model;
		}

		@StringSetter(MODEL)
		public void setModel(String model) {
			this.model = model;
		}
	}
}
