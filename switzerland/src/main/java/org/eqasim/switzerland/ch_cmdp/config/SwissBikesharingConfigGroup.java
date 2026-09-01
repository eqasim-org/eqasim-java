package org.eqasim.switzerland.ch_cmdp.config;

import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class SwissBikesharingConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "swissBikesharing";

	private static final String USE_BIKESHARING = "useBikesharing";
	private static final String CHF_KM = "CHF_km";
	private static final String CHF_MIN = "CHF_min";
	private static final String CHF_BASE = "CHF_base";
	private static final String CHF_MINIMUM = "CHF_minimum";

	private boolean useBikesharing = false;
	private double CHF_km = 0.26;
	private double CHF_min = 0.0;
	private double CHF_base = 1.0;
	private double CHF_minimum = 1.5;

	public SwissBikesharingConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(USE_BIKESHARING, "Whether bikesharing is enabled.");
		map.put(CHF_KM, "Bikesharing cost per kilometer in CHF.");
		map.put(CHF_MIN, "Bikesharing cost per minute in CHF.");
		map.put(CHF_BASE, "Bikesharing base fare in CHF.");
		map.put(CHF_MINIMUM, "Bikesharing minimum fare in CHF.");
		return map;
	}

	@StringGetter(USE_BIKESHARING)
	public boolean isUseBikesharing() {
		return useBikesharing;
	}

	@StringSetter(USE_BIKESHARING)
	public void setUseBikesharing(boolean useBikesharing) {
		this.useBikesharing = useBikesharing;
	}

	@StringGetter(CHF_KM)
	public double getCHF_km() {
		return CHF_km;
	}

	@StringSetter(CHF_KM)
	public void setCHF_km(double CHF_km) {
		this.CHF_km = CHF_km;
	}

	@StringGetter(CHF_MIN)
	public double getCHF_min() {
		return CHF_min;
	}

	@StringSetter(CHF_MIN)
	public void setCHF_min(double CHF_min) {
		this.CHF_min = CHF_min;
	}

	@StringGetter(CHF_BASE)
	public double getCHF_base() {
		return CHF_base;
	}

	@StringSetter(CHF_BASE)
	public void setCHF_base(double CHF_base) {
		this.CHF_base = CHF_base;
	}

	@StringGetter(CHF_MINIMUM)
	public double getCHF_minimum() {
		return CHF_minimum;
	}

	@StringSetter(CHF_MINIMUM)
	public void setCHF_minimum(double CHF_minimum) {
		this.CHF_minimum = CHF_minimum;
	}

	public static SwissBikesharingConfigGroup getOrCreate(Config config) {
		SwissBikesharingConfigGroup group = (SwissBikesharingConfigGroup) config.getModules().get(GROUP_NAME);

		if (group == null) {
			group = new SwissBikesharingConfigGroup();
			config.addModule(group);
		}

		return group;
	}
}
