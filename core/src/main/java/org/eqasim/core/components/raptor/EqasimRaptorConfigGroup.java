package org.eqasim.core.components.raptor;

import org.matsim.core.config.ReflectiveConfigGroup;

public class EqasimRaptorConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:raptor";

	public EqasimRaptorConfigGroup() {
		super(GROUP_NAME);
	}

	@Parameter
	public double travelTimeRail_u_h = -7.0;

	@Parameter
	public double travelTimeSubway_u_h = -7.0;

	@Parameter
	public double travelTimeBus_u_h = -7.0;

	@Parameter
	public double travelTimeTram_u_h = -7.0;

	@Parameter
	public double travelTimeOther_u_h = -7.0;

	@Parameter
	public double perTransfer_u = -1.0;

	@Parameter
	public double waitTime_u_h = -6.0;

	@Parameter
	public double walkTime_u_h = -7.0;
}
