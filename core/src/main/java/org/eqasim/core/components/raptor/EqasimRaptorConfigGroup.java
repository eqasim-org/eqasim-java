package org.eqasim.core.components.raptor;

import org.matsim.core.config.ReflectiveConfigGroup;

public class EqasimRaptorConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:raptor";

	public EqasimRaptorConfigGroup() {
		super(GROUP_NAME);
	}

	@Parameter
	public double travelTimeRail_u_h = -1.4278139352278472;

	@Parameter
	public double travelTimeSubway_u_h = -1.0;

	@Parameter
	public double travelTimeBus_u_h = -2.835025304050246;

	@Parameter
	public double travelTimeTram_u_h = -3.199594607188756;

	@Parameter
	public double travelTimeOther_u_h = -2.835025304050246;

	@Parameter
	public double perTransfer_u = -0.5441109013512305;

	@Parameter
	public double waitTime_u_h = -0.497984826174775;

	@Parameter
	public double walkTime_u_h = -3.8494071051697385;
}
