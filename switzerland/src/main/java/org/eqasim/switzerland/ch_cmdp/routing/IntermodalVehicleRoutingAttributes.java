package org.eqasim.switzerland.ch_cmdp.routing;

/**
 * Routing attributes used to pass intermodal vehicle continuity requirements
 * from discrete mode choice into SwissRailRaptor stop selection.
 */

public final class IntermodalVehicleRoutingAttributes {
	// When present, the stop finder keeps only access candidates with this
	// private-vehicle mode and, optionally, this exact boarding stop.
	static public final String REQUIRED_ACCESS_MODE = "iv:reqAccessMode";
	static public final String REQUIRED_ACCESS_STOP_ID = "iv:reqAccessStop";
	static public final String FORBIDDEN_ACCESS_MODE = "iv:forbidAccessMode";

	// When present, the stop finder keeps only egress candidates with this
	// private-vehicle mode and, optionally, this exact alighting stop.
	static public final String REQUIRED_EGRESS_MODE = "iv:reqEgressMode";
	static public final String REQUIRED_EGRESS_STOP_ID = "iv:reqEgressStop";
	static public final String FORBIDDEN_EGRESS_MODE = "iv:forbidEgressMode";

}
