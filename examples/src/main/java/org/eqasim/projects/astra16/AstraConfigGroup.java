package org.eqasim.projects.astra16;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class AstraConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "astra";

	static public final String FLEET_SIZE = "fleetSize";

	private int fleetSize = 0;

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

	static public AstraConfigGroup get(Config config) {
		if (!config.getModules().containsKey(GROUP_NAME)) {
			config.addModule(new AstraConfigGroup());
		}

		return (AstraConfigGroup) config.getModules().get(GROUP_NAME);
	}
}
