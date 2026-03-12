package org.eqasim.core.simulation.vdf.engine;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Verify;

public class VDFEngineConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:vdf_engine";

	@Parameter
	private Set<String> modes = new HashSet<>(Set.of(TransportMode.car));

	@Parameter
	private Set<String> dynamicModes = new HashSet<>();

	@Parameter
	private int generateNetworkEventsInterval = 1;

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

	public Set<String> getDynamicModes() {
		return dynamicModes;
	}

	public void setDynamicModes(Set<String> dynamicModes) {
		this.dynamicModes.clear();
		this.dynamicModes.addAll(dynamicModes);
	}

	public int getGenerateNetworkEventsInterval() {
		return generateNetworkEventsInterval;
	}

	public void setGenerateNetworkEventsInterval(int val) {
		this.generateNetworkEventsInterval = val;
	}

	public static VDFEngineConfigGroup getOrCreate(Config config) {
		VDFEngineConfigGroup group = (VDFEngineConfigGroup) config.getModules().get(GROUP_NAME);

		if (group == null) {
			group = new VDFEngineConfigGroup();
			config.addModule(group);
		}

		return group;
	}

	@Override
	public void checkConsistency(Config config) {
		super.checkConsistency(config);

		Verify.verify(modes.containsAll(dynamicModes), "The dynamic modes must be a subset of the active vdf modes");

		for (String mode : modes) {
			Verify.verify(!config.qsim().getMainModes().contains(mode),
					"VDF mode '" + mode + "'' should not be contained in the qsim main modes.");
		}

		for (String mode : modes) {
			Verify.verify(config.routing().getNetworkModes().contains(mode),
					"VDF mode '" + mode + "'' should be contained in the routing network modes.");
		}
	}
}
