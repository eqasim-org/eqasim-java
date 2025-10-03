package org.eqasim.core.components.raptor;

import org.matsim.core.config.ReflectiveConfigGroup;

public class EqasimRaptorConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:raptor";

	public EqasimRaptorConfigGroup() {
		super(GROUP_NAME);
	}

	@Parameter
	public double travelTimeRail_u_h = -0.08317394412379128;

	@Parameter
	public double travelTimeSubway_u_h = -1.0;

	@Parameter
	public double travelTimeBus_u_h = -2.8470557962683523;

	@Parameter
	public double travelTimeTram_u_h = -4.849609430935352;

	@Parameter
	public double travelTimeOther_u_h = -2.8470557962683523;

	@Parameter
	public double perTransfer_u = -0.47539778048347203;

	@Parameter
	public double waitTime_u_h = -17.935075050105493;

	@Parameter
	public double walkTime_u_h = -4.198783720934392;
}
