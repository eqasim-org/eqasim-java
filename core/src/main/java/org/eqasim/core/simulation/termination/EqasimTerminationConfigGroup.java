package org.eqasim.core.simulation.termination;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class EqasimTerminationConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "eqasim:termination";

	public EqasimTerminationConfigGroup() {
		super(GROUP_NAME);
	}

	static private final String HORIZON = "horizon";
	static private final String SMOOTHING = "smoothing";
	static private final String THRESHOLD = "threshold";
	static private final String MODES = "modes";
	static private final String HISTORY_FILE = "historyFile";

	private int horizon = 10;
	private int smoothing = 20;
	private double threshold = 0.001;

	private List<String> modes = new LinkedList<>(Arrays.asList("car", "pt", "bike", "walk"));

	private String historyFile = null;

	@StringGetter(HORIZON)
	public int getHorizon() {
		return horizon;
	}

	@StringSetter(HORIZON)
	public void setHorizon(int value) {
		this.horizon = value;
	}

	@StringGetter(SMOOTHING)
	public int getSmoothing() {
		return smoothing;
	}

	@StringSetter(SMOOTHING)
	public void setSmoothing(int value) {
		this.smoothing = value;
	}

	@StringGetter(THRESHOLD)
	public double getThreshold() {
		return threshold;
	}

	@StringSetter(THRESHOLD)
	public void setThreshold(double value) {
		this.threshold = value;
	}

	@StringGetter(MODES)
	public String getModesAsString() {
		return String.join(",", modes);
	}

	@StringSetter(MODES)
	public void setModesAsString(String value) {
		modes.clear();
		modes.addAll(Arrays.asList(value.split(",")).stream().map(String::trim).collect(Collectors.toList()));
	}

	public List<String> getModes() {
		return modes;
	}

	public void setModes(List<String> value) {
		modes.clear();
		modes.addAll(value);
	}

	@StringSetter(HISTORY_FILE)
	public void setHistoryFile(String value) {
		this.historyFile = value;
	}

	@StringGetter(HISTORY_FILE)
	public String getHistoryFile() {
		return historyFile;
	}

	static public EqasimTerminationConfigGroup getOrCreate(Config config) {
		if (!config.getModules().containsKey(GROUP_NAME)) {
			config.addModule(new EqasimTerminationConfigGroup());
		}

		return (EqasimTerminationConfigGroup) config.getModules().get(GROUP_NAME);
	}
}
