package org.eqasim.automated_vehicles.components;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class EqasimAvConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "eqasim:av";

	private final static String MODE_PARAMETERS_PATH = "modeParametersPath";
	private final static String COST_PARAMETERS_PATH = "costParametersPath";

	private String modeParametersPath = null;
	private String costParametersPath = null;

	public EqasimAvConfigGroup() {
		super(GROUP_NAME);
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

	static public EqasimAvConfigGroup getOrCreate(Config config) {
		EqasimAvConfigGroup configGroup = (EqasimAvConfigGroup) config.getModules().get(GROUP_NAME);

		if (configGroup == null) {
			configGroup = new EqasimAvConfigGroup();
			config.addModule(configGroup);
		}

		return configGroup;
	}
}
