package org.eqasim.projects.astra16;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class AstraConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "astra";

	static public final String FLEET_SIZE = "fleetSize";
	static public final String OPERATING_AREA_PATH = "operatingAreaPath";
	static public final String OPERATING_AREA_INDEX_ATTRIBUTE = "operatingAreaIndexAttribute";

	private int fleetSize = 0;
	private String operatingAreaPath = null;
	private String operatingAreaIndexAttribute = "wgIndex";

	public AstraConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(FLEET_SIZE)
	public int getFleetSize() {
		return fleetSize;
	}

	@StringSetter(FLEET_SIZE)
	public void setFleetSize(int fleetSize) {
		this.fleetSize = fleetSize;
	}

	@StringGetter(OPERATING_AREA_PATH)
	public String getOperatingAreaPath() {
		return operatingAreaPath;
	}

	@StringSetter(OPERATING_AREA_PATH)
	public void setOperatingAreaPath(String operatingAreaPath) {
		this.operatingAreaPath = operatingAreaPath;
	}

	@StringGetter(OPERATING_AREA_INDEX_ATTRIBUTE)
	public String getOperatingAreaIndexAttribute() {
		return operatingAreaIndexAttribute;
	}

	@StringSetter(OPERATING_AREA_INDEX_ATTRIBUTE)
	public void setOperatingAreaIndexAttribute(String operatingAreaIndexAttribute) {
		this.operatingAreaIndexAttribute = operatingAreaIndexAttribute;
	}

	static public AstraConfigGroup get(Config config) {
		if (!config.getModules().containsKey(GROUP_NAME)) {
			config.addModule(new AstraConfigGroup());
		}

		return (AstraConfigGroup) config.getModules().get(GROUP_NAME);
	}
}
