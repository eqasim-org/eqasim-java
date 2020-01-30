package org.eqasim.projects.dynamic_av;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class ProjectConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "project";

	public final static String OPERATING_AREA_PATH = "operatingAreaPath";
	public final static String WAITING_TIME_GROUP_INDEX_ATTRIBUTE = "waitingTimeGroupIndexAttribute";
	public final static String USE_AV = "useAv";
	
	public final static String MINIMUM_DISTANCE_KM = "minimumDistance_km";
	public final static String MAXIMUM_WAIT_TIME_MIN = "maximumWaitTime_min";

	private String operatingAreaPath = null;
	private String waitingTimeGroupIndexAttribute = "wgIndex";
	private boolean useAv = true;
	
	private double minimumDistance_km = 0.5;
	private double maximumWaitTime_min = 15.0;

	public ProjectConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(OPERATING_AREA_PATH)
	public String getOperatingAreaPath() {
		return operatingAreaPath;
	}

	@StringSetter(OPERATING_AREA_PATH)
	public void setOperatingAreaPath(String operatingAreaPath) {
		this.operatingAreaPath = operatingAreaPath;
	}

	@StringGetter(WAITING_TIME_GROUP_INDEX_ATTRIBUTE)
	public String getWaitingTimeGroupIndexAttribute() {
		return waitingTimeGroupIndexAttribute;
	}

	@StringSetter(WAITING_TIME_GROUP_INDEX_ATTRIBUTE)
	public void setWaitingTimeGroupIndexAttribute(String waitingTimeGroupIndexAttribute) {
		this.waitingTimeGroupIndexAttribute = waitingTimeGroupIndexAttribute;
	}

	@StringGetter(USE_AV)
	public boolean getUseAv() {
		return useAv;
	}

	@StringSetter(USE_AV)
	public void setUseAv(boolean useAv) {
		this.useAv = useAv;
	}
	
	@StringGetter(MINIMUM_DISTANCE_KM)
	public double getMinimumDistance_km() {
		return minimumDistance_km;
	}

	@StringSetter(MINIMUM_DISTANCE_KM)
	public void setMinimumDistance_km(double minimumDistance_km) {
		this.minimumDistance_km = minimumDistance_km;
	}
	
	@StringGetter(MAXIMUM_WAIT_TIME_MIN)
	public double getMaximumWaitTime_min() {
		return maximumWaitTime_min;
	}

	@StringSetter(MAXIMUM_WAIT_TIME_MIN)
	public void setMaximumWaitTime_min(double maximumWaitTime_min) {
		this.maximumWaitTime_min = maximumWaitTime_min;
	}

	static public ProjectConfigGroup get(Config config) {
		if (!config.getModules().containsKey(GROUP_NAME)) {
			config.addModule(new ProjectConfigGroup());
		}

		return (ProjectConfigGroup) config.getModules().get(GROUP_NAME);
	}
}
