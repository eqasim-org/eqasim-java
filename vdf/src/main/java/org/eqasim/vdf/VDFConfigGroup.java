package org.eqasim.vdf;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

public class VDFConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:vdf";

	static private final String START_TIME = "startTime";
	static private final String END_TIME = "endTime";
	static private final String INTERVAL = "interval";
	static private final String MINIMUM_SPEED = "minimumSpeed";
	static private final String HORIZON = "horizon";

	static private final String BPR_FACTOR = "bpr:factor";
	static private final String BPR_EXPONENT = "bpr:exponent";

	private double startTime = 0.0 * 3600.0;
	private double endTime = 24.0 * 3600.0;
	private double interval = 3600.0;
	private double minimumSpeed = 5.0 / 3.6;
	private int horizon = 10;

	private double bprFactor = 0.15;
	private double bprExponent = 4.0;

	public VDFConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(START_TIME)
	public String getStartTimeAsString() {
		return Time.writeTime(startTime);
	}

	public double getStartTime() {
		return startTime;
	}

	@StringSetter(START_TIME)
	public void setStartTimeAsString(String startTime) {
		this.startTime = Time.parseTime(startTime);
	}

	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	@StringGetter(END_TIME)
	public String getEndTimeAsString() {
		return Time.writeTime(endTime);
	}

	public double getEndTime() {
		return endTime;
	}

	@StringSetter(END_TIME)
	public void setEndTimeAsString(String endTime) {
		this.endTime = Time.parseTime(endTime);
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	@StringGetter(INTERVAL)
	public String getIntervalAsString() {
		return Time.writeTime(interval);
	}

	public double getInterval() {
		return interval;
	}

	@StringSetter(INTERVAL)
	public void setIntervalAsString(String interval) {
		this.interval = Time.parseTime(interval);
	}

	public void setInterval(double interval) {
		this.interval = interval;
	}

	@StringGetter(MINIMUM_SPEED)
	public double getMinimumSpeed() {
		return minimumSpeed;
	}

	@StringSetter(MINIMUM_SPEED)
	public void setMinimumSpeed(double minimumSpeed) {
		this.minimumSpeed = minimumSpeed;
	}

	@StringGetter(HORIZON)
	public int getHorizon() {
		return horizon;
	}

	@StringSetter(HORIZON)
	public void setHorizon(int horizon) {
		this.horizon = horizon;
	}

	@StringGetter(BPR_FACTOR)
	public double getBprFactor() {
		return bprFactor;
	}

	@StringSetter(BPR_FACTOR)
	public void setBprFactor(double bprFactor) {
		this.bprFactor = bprFactor;
	}

	@StringGetter(BPR_EXPONENT)
	public double getBprExponent() {
		return bprExponent;
	}

	@StringSetter(BPR_EXPONENT)
	public void setBprExponent(double bprExponent) {
		this.bprExponent = bprExponent;
	}
}
