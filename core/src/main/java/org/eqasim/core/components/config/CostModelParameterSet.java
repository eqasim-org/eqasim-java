package org.eqasim.core.components.config;

import org.matsim.core.config.ReflectiveConfigGroup;

class CostModelParameterSet extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "cost_model";

	static public final String MODE = "mode";
	static public final String MODEL = "model";

	private String mode;
	private String model;

	public CostModelParameterSet() {
		super(GROUP_NAME);
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
