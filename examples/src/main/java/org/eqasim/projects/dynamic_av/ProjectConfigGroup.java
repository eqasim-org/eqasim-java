package org.eqasim.projects.dynamic_av;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class ProjectConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "project";

	public final static String OPERATING_AREA_PATH = "operatingAreaPath";
	public final static String WAITING_TIME_GROUP_INDEX_ATTRIBUTE = "waitingTimeGroupIndexAttribute";
	public final static String USE_AV = "useAv";

	private String operatingAreaPath = null;
	private String waitingTimeGroupIndexAttribute = "wgIndex";
	private boolean useAv = true;

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

	static public ProjectConfigGroup get(Config config) {
		if (!config.getModules().containsKey(GROUP_NAME)) {
			config.addModule(new ProjectConfigGroup());
		}

		return (ProjectConfigGroup) config.getModules().get(GROUP_NAME);
	}

}
