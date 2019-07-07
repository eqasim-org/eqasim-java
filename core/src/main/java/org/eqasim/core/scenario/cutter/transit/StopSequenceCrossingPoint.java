package org.eqasim.core.scenario.cutter.transit;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class StopSequenceCrossingPoint {
	final public boolean isOutgoing;

	final public TransitRouteStop insideStop;
	final public TransitRouteStop outsideStop;
	
	final public int index;

	public StopSequenceCrossingPoint(TransitRouteStop insideStop, TransitRouteStop outsideStop, boolean isOutgoing, int index) {
		this.isOutgoing = isOutgoing;
		this.insideStop = insideStop;
		this.outsideStop = outsideStop;
		this.index = index;
	}
}
