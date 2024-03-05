package org.eqasim.vdf;

import java.util.List;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;

import com.google.common.base.Verify;

public class VDFScope {
	private final double startTime;
	private final double endTime;
	private final double intervalTime;

	private final int intervals;

	public VDFScope(double startTime, double endTime, double intervalTime) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.intervalTime = intervalTime;
		this.intervals = (int) Math.floor((endTime - startTime) / intervalTime) + 1;
	}

	public int getIntervals() {
		return intervals;
	}

	public int getIntervalIndex(double time) {
		return Math.min(Math.max(0, (int) Math.floor((time - startTime) / intervalTime)), intervals - 1);
	}

	public double getIntervalTime() {
		return intervalTime;
	}

	public double getStartTime() {
		return startTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void verify(List<?> values, String reason) {
		Verify.verify(values.size() == intervals, reason);
	}

	public void verify(IdMap<Link, List<Double>> values, String reason) {
		for (var item : values.values()) {
			Verify.verify(item.size() == intervals, reason);
		}
	}
}
