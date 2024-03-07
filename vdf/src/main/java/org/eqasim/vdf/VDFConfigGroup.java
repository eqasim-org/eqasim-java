package org.eqasim.vdf;

import java.util.Arrays;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

public class VDFConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:vdf";

	static private final String START_TIME = "startTime";
	static private final String END_TIME = "endTime";
	static private final String INTERVAL = "interval";
	static private final String MINIMUM_SPEED = "minimumSpeed";
	static private final String HORIZON = "horizon";
	static private final String CAPACITY_FACTOR = "capacityFactor";

	static private final String BPR_FACTOR = "bpr:factor";
	static private final String BPR_EXPONENT = "bpr:exponent";

	static private final String MODES = "modes";

	static private final String HANDLER = "handler";
	static private final String INPUT_FILE = "inputFile";
	static private final String WRITE_INTERVAL = "writeInterval";
	static private final String WRITE_FLOW_INTERVAL = "writeFlowInterval";
	
	static private final String GENERATE_NETWORK_EVENTS = "generateNetworkEvents";

	private double startTime = 0.0 * 3600.0;
	private double endTime = 24.0 * 3600.0;
	private double interval = 3600.0;
	private double minimumSpeed = 5.0 / 3.6;
	private int horizon = 10;

	private double bprFactor = 0.15;
	private double bprExponent = 4.0;

	private Set<String> modes = Set.of(TransportMode.car, "car_passenger");

	private double capacityFactor = 1.0;

	private String inputFile = null;
	private int writeInterval = 0;
	private int writeFlowInterval = 0;
	
	private boolean generateNetworkEvents = true;

	public enum HandlerType {
		Horizon, Interpolation
	}

	private HandlerType handler = HandlerType.Horizon;

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

	@StringGetter(CAPACITY_FACTOR)
	public double getCapacityFactor() {
		return capacityFactor;
	}

	@StringSetter(CAPACITY_FACTOR)
	public void setCapacityFactor(double capacityFactor) {
		this.capacityFactor = capacityFactor;
	}

	public Set<String> getModes() {
		return modes;
	}

	public void setModes(Set<String> modes) {
		this.modes.clear();
		this.modes.addAll(modes);
	}

	@StringGetter(MODES)
	public String getModesAsString() {
		return String.join(",", modes);
	}

	@StringSetter(MODES)
	public void setModesAsString(String modes) {
		this.modes.clear();
		Arrays.asList(modes.split(",")).stream().map(String::trim).forEach(this.modes::add);
	}

	@StringGetter(HANDLER)
	public HandlerType getHandler() {
		return handler;
	}

	@StringSetter(HANDLER)
	public void setHandler(HandlerType handler) {
		this.handler = handler;
	}

	@StringGetter(INPUT_FILE)
	public String getInputFile() {
		return inputFile;
	}

	@StringSetter(INPUT_FILE)
	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}

	@StringGetter(WRITE_INTERVAL)
	public int getWriteInterval() {
		return writeInterval;
	}

	@StringSetter(WRITE_INTERVAL)
	public void setWriteInterval(int writeInterval) {
		this.writeInterval = writeInterval;
	}

	@StringGetter(WRITE_FLOW_INTERVAL)
	public int getWriteFlowInterval() {
		return writeFlowInterval;
	}

	@StringSetter(WRITE_FLOW_INTERVAL)
	public void setWriteFlowInterval(int val) {
		this.writeFlowInterval = val;
	}
	
	@StringGetter(GENERATE_NETWORK_EVENTS)
	public boolean getNetworkEvents() {
		return generateNetworkEvents;
	}

	@StringSetter(GENERATE_NETWORK_EVENTS)
	public void setNetworkEvents(boolean val) {
		this.generateNetworkEvents = val;
	}

	public static VDFConfigGroup getOrCreate(Config config) {
		VDFConfigGroup group = (VDFConfigGroup) config.getModules().get(GROUP_NAME);

		if (group == null) {
			group = new VDFConfigGroup();
			config.addModule(group);
		}

		return group;
	}
}
