package org.eqasim.core.simulation.calibration;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class CalibrationConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:calibration";

	static public final String REFERENCE_PATH = "referencePath";
	static public final String ENABLE = "enableOutput";
	static public final String HINT = "hint";

	private String referencePath;
	private boolean enable = false;
	private String hint = "No hint given.";

	public CalibrationConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(REFERENCE_PATH)
	public String getReferencePath() {
		return referencePath;
	}

	@StringSetter(REFERENCE_PATH)
	public void setReferencePath(String referencePath) {
		this.referencePath = referencePath;
	}

	@StringGetter(ENABLE)
	public boolean getEnable() {
		return enable;
	}

	@StringSetter(ENABLE)
	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	@StringGetter(HINT)
	public String getHint() {
		return hint;
	}

	@StringSetter(HINT)
	public void setHint(String hint) {
		this.hint = hint;
	}

	static public CalibrationConfigGroup get(Config config) {
		if (!config.getModules().containsKey(GROUP_NAME)) {
			config.addModule(new CalibrationConfigGroup());
		}

		return (CalibrationConfigGroup) config.getModules().get(GROUP_NAME);
	}
}
