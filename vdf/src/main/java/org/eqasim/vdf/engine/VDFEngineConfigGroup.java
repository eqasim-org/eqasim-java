package org.eqasim.vdf.engine;

import java.util.Arrays;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class VDFEngineConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:vdf_engine";

	static private final String MODES = "modes";
	static private final String GENERATE_NETWORK_EVENTS = "generateNetworkEvents";

	private Set<String> modes = Set.of(TransportMode.car);

	private boolean generateNetworkEvents = true;

	public VDFEngineConfigGroup() {
		super(GROUP_NAME);
	}

	public Set<String> getModes() {
		return modes;
	}

	public void setModes(Set<String> modes) {
		this.modes.clear();
		this.modes.addAll(modes);
	}

	@StringGetter(MODES)
	public String getModesAsString() {
		return String.join(",", modes);
	}

	@StringSetter(MODES)
	public void setModesAsString(String modes) {
		this.modes.clear();
		Arrays.asList(modes.split(",")).stream().map(String::trim).forEach(this.modes::add);
	}

	@StringGetter(GENERATE_NETWORK_EVENTS)
	public boolean getGenerateNetworkEvents() {
		return generateNetworkEvents;
	}

	@StringSetter(GENERATE_NETWORK_EVENTS)
	public void setGenerateNetworkEvents(boolean val) {
		this.generateNetworkEvents = val;
	}

	public static VDFEngineConfigGroup getOrCreate(Config config) {
		VDFEngineConfigGroup group = (VDFEngineConfigGroup) config.getModules().get(GROUP_NAME);

		if (group == null) {
			group = new VDFEngineConfigGroup();
			config.addModule(group);
		}

		return group;
	}
}
