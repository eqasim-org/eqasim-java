package org.eqasim.switzerland.ch_cmdp.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class SwissIntermodalAccessEgressConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "swissIntermodalAccessEgress";

	static private final String UTILITY_ERROR_SCALE = "utilityErrorScale";
	static private final String UTILITY_ERROR_MODES = "utilityErrorModes";
	static private final String USE_INTERMODAL_PT_PREDICTOR = "useIntermodalPtPredictor";

	private double utilityErrorScale = 0.0;
	private String utilityErrorModes = "";
	private boolean useIntermodalPtPredictor = false;

	public SwissIntermodalAccessEgressConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(UTILITY_ERROR_SCALE)
	public double getUtilityErrorScale() {
		return utilityErrorScale;
	}

	@StringSetter(UTILITY_ERROR_SCALE)
	public void setUtilityErrorScale(double utilityErrorScale) {
		if (utilityErrorScale < 0.0) {
			throw new IllegalArgumentException("Utility error scale must be non-negative.");
		}

		this.utilityErrorScale = utilityErrorScale;
	}

	@StringGetter(UTILITY_ERROR_MODES)
	public String getUtilityErrorModesAsString() {
		return utilityErrorModes;
	}

	@StringSetter(UTILITY_ERROR_MODES)
	public void setUtilityErrorModes(String utilityErrorModes) {
		this.utilityErrorModes = utilityErrorModes == null ? "" : utilityErrorModes.trim();
	}

	public Set<String> getUtilityErrorModes() {
		if (utilityErrorModes.isBlank()) {
			return Collections.emptySet();
		}

		return Arrays.stream(utilityErrorModes.split(",")) //
				.map(String::trim) //
				.filter(mode -> !mode.isEmpty()) //
				.collect(Collectors.toSet());
	}

	@StringGetter(USE_INTERMODAL_PT_PREDICTOR)
	public boolean useIntermodalPtPredictor() {
		return useIntermodalPtPredictor;
	}

	@StringSetter(USE_INTERMODAL_PT_PREDICTOR)
	public void setUseIntermodalPtPredictor(boolean useIntermodalPtPredictor) {
		this.useIntermodalPtPredictor = useIntermodalPtPredictor;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(UTILITY_ERROR_SCALE,
				"Scale parameter of the person-specific Gumbel utility error added to intermodal access/egress modes. Zero disables the error.");
		comments.put(UTILITY_ERROR_MODES,
				"Comma-separated access/egress modes that receive a utility error. Leave empty to apply it to all access/egress leg modes.");
		comments.put(USE_INTERMODAL_PT_PREDICTOR,
				"Whether to use the ch_cmdp intermodal PT predictor. Set to false to keep Eqasim's original PT predictor.");
		return comments;
	}

	static public SwissIntermodalAccessEgressConfigGroup getOrCreate(Config config) {
		SwissIntermodalAccessEgressConfigGroup group = (SwissIntermodalAccessEgressConfigGroup) config.getModules()
				.get(GROUP_NAME);

		if (group == null) {
			group = new SwissIntermodalAccessEgressConfigGroup();
			config.addModule(group);
		}

		return group;
	}
}
