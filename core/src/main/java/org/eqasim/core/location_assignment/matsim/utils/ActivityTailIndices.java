package org.eqasim.core.location_assignment.matsim.utils;

public class ActivityTailIndices {
	final private int anchorActivityIndex;
	final private int tailActivtiyIndex;

	public ActivityTailIndices(int anchorActivityIndex, int tailActivtiyIndex) {
		this.anchorActivityIndex = anchorActivityIndex;
		this.tailActivtiyIndex = tailActivtiyIndex;
	}

	public int getAnchorActivtiyIndex() {
		return anchorActivityIndex;
	}

	public int getTailActivityIndex() {
		return tailActivtiyIndex;
	}
}
