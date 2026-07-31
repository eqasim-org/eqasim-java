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
	static private final String RESTRICT_BIKE_TO_HOME_ACTIVITY = "restrictBikeToHomeActivity";
	static private final String BIKE_RESTRICTED_ACTIVITY_TYPE = "bikeRestrictedActivityType";
	static private final String BIKE_RESTRICTED_MODE = "bikeRestrictedMode";
	static private final String ENFORCE_INTERMODAL_VEHICLE_CONTINUITY_DURING_ROUTING = "enforceIntermodalVehicleContinuityDuringRouting";
	static private final String INTERMODAL_VEHICLE_CONTINUITY_HOME_ACTIVITY_TYPE = "intermodalVehicleContinuityHomeActivityType";

	private double utilityErrorScale = 0.0;
	private String utilityErrorModes = "";
	private boolean useIntermodalPtPredictor = false;
	private boolean restrictBikeToHomeActivity = true;
	private String bikeRestrictedActivityType = "home";
	private String bikeRestrictedMode = "bike";
	private boolean enforceIntermodalVehicleContinuityDuringRouting = false;
	private String intermodalVehicleContinuityHomeActivityType = "home";

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

	@StringGetter(RESTRICT_BIKE_TO_HOME_ACTIVITY)
	public boolean restrictBikeToHomeActivity() {
		return restrictBikeToHomeActivity;
	}

	@StringSetter(RESTRICT_BIKE_TO_HOME_ACTIVITY)
	public void setRestrictBikeToHomeActivity(boolean restrictBikeToHomeActivity) {
		this.restrictBikeToHomeActivity = restrictBikeToHomeActivity;
	}

	@StringGetter(BIKE_RESTRICTED_ACTIVITY_TYPE)
	public String getBikeRestrictedActivityType() {
		return bikeRestrictedActivityType;
	}

	@StringSetter(BIKE_RESTRICTED_ACTIVITY_TYPE)
	public void setBikeRestrictedActivityType(String bikeRestrictedActivityType) {
		if (bikeRestrictedActivityType == null || bikeRestrictedActivityType.isBlank()) {
			throw new IllegalArgumentException("Bike restricted activity type must not be empty.");
		}

		this.bikeRestrictedActivityType = bikeRestrictedActivityType.trim();
	}

	@StringGetter(BIKE_RESTRICTED_MODE)
	public String getBikeRestrictedMode() {
		return bikeRestrictedMode;
	}

	@StringSetter(BIKE_RESTRICTED_MODE)
	public void setBikeRestrictedMode(String bikeRestrictedMode) {
		if (bikeRestrictedMode == null || bikeRestrictedMode.isBlank()) {
			throw new IllegalArgumentException("Bike restricted mode must not be empty.");
		}

		this.bikeRestrictedMode = bikeRestrictedMode.trim();
	}

	@StringGetter(ENFORCE_INTERMODAL_VEHICLE_CONTINUITY_DURING_ROUTING)
	public boolean enforceIntermodalVehicleContinuityDuringRouting() {
		return enforceIntermodalVehicleContinuityDuringRouting;
	}

	@StringSetter(ENFORCE_INTERMODAL_VEHICLE_CONTINUITY_DURING_ROUTING)
	public void setEnforceIntermodalVehicleContinuityDuringRouting(
			boolean enforceIntermodalVehicleContinuityDuringRouting) {
		this.enforceIntermodalVehicleContinuityDuringRouting = enforceIntermodalVehicleContinuityDuringRouting;
	}

	@StringGetter(INTERMODAL_VEHICLE_CONTINUITY_HOME_ACTIVITY_TYPE)
	public String getIntermodalVehicleContinuityHomeActivityType() {
		return intermodalVehicleContinuityHomeActivityType;
	}

	@StringSetter(INTERMODAL_VEHICLE_CONTINUITY_HOME_ACTIVITY_TYPE)
	public void setIntermodalVehicleContinuityHomeActivityType(String intermodalVehicleContinuityHomeActivityType) {
		if (intermodalVehicleContinuityHomeActivityType == null
				|| intermodalVehicleContinuityHomeActivityType.isBlank()) {
			throw new IllegalArgumentException("Intermodal vehicle continuity home activity type must not be empty.");
		}

		this.intermodalVehicleContinuityHomeActivityType = intermodalVehicleContinuityHomeActivityType.trim();
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
		comments.put(RESTRICT_BIKE_TO_HOME_ACTIVITY,
				"If true, bike intermodal access is only allowed for trips starting at the configured activity type, and bike egress only for trips ending at that activity type.");
		comments.put(BIKE_RESTRICTED_ACTIVITY_TYPE,
				"Activity type that allows bike access from trip origin and bike egress to trip destination.");
		comments.put(BIKE_RESTRICTED_MODE, "Intermodal access/egress mode to restrict to the configured activity type.");
		comments.put(ENFORCE_INTERMODAL_VEHICLE_CONTINUITY_DURING_ROUTING,
				"If true, routed PT candidates are constrained to retrieve an intermodal private vehicle at the same stop where it was left before returning home.");
		comments.put(INTERMODAL_VEHICLE_CONTINUITY_HOME_ACTIVITY_TYPE,
				"Destination activity type where an intermodal private vehicle must be retrieved if it was left at a PT stop earlier in the tour.");
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
