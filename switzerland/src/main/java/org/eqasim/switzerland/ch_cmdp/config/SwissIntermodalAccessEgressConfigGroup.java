package org.eqasim.switzerland.ch_cmdp.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class SwissIntermodalAccessEgressConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "swissIntermodalAccessEgress";

	static private final String UTILITY_ERROR_SCALE = "utilityErrorScale";
	static private final String UTILITY_ERROR_MODES = "utilityErrorModes";
	static private final String RESTRICT_VEHICLE_TO_HOME_ACTIVITY = "restrictVehicleToHomeActivity";
	static private final String VEHICLE_RESTRICTED_ACTIVITY_TYPE = "vehicleRestrictedActivityType";
	static private final String RESTRICTED_INTERMODAL_ACCESS_EGRESS_MODES = "restrictedIntermodalAccessEgressModes";
	static private final String ENFORCE_INTERMODAL_VEHICLE_CONTINUITY_DURING_ROUTING = "enforceIntermodalVehicleContinuityDuringRouting";
	static private final String INTERMODAL_VEHICLE_CONTINUITY_HOME_ACTIVITY_TYPE = "intermodalVehicleContinuityHomeActivityType";

	private double utilityErrorScale = 0.0;
	private String utilityErrorModes = "";
	private boolean restrictVehicleToHomeActivity = true;
	private String vehicleRestrictedActivityType = "home";
	private String restrictedIntermodalAccessEgressModes = TransportMode.bike;
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

	@StringGetter(RESTRICT_VEHICLE_TO_HOME_ACTIVITY)
	public boolean restrictVehicleToHomeActivity() {
		return restrictVehicleToHomeActivity;
	}

	@StringSetter(RESTRICT_VEHICLE_TO_HOME_ACTIVITY)
	public void setRestrictVehicleToHomeActivity(boolean restrictVehicleToHomeActivity) {
		this.restrictVehicleToHomeActivity = restrictVehicleToHomeActivity;
	}

	@StringGetter(VEHICLE_RESTRICTED_ACTIVITY_TYPE)
	public String getVehicleRestrictedActivityType() {
		return vehicleRestrictedActivityType;
	}

	@StringSetter(VEHICLE_RESTRICTED_ACTIVITY_TYPE)
	public void setVehicleRestrictedActivityType(String vehicleRestrictedActivityType) {
		if (vehicleRestrictedActivityType == null || vehicleRestrictedActivityType.isBlank()) {
			throw new IllegalArgumentException("Vehicle restricted activity type must not be empty.");
		}

		this.vehicleRestrictedActivityType = vehicleRestrictedActivityType.trim();
	}

	@StringGetter(RESTRICTED_INTERMODAL_ACCESS_EGRESS_MODES)
	public String getRestrictedIntermodalAccessEgressModesAsString() {
		return restrictedIntermodalAccessEgressModes;
	}

	@StringSetter(RESTRICTED_INTERMODAL_ACCESS_EGRESS_MODES)
	public void setRestrictedIntermodalAccessEgressModes(String restrictedIntermodalAccessEgressModes) {
		this.restrictedIntermodalAccessEgressModes = normalizeModeList(restrictedIntermodalAccessEgressModes,
				"Restricted intermodal access/egress modes must not be empty.");
	}

	public Set<String> getRestrictedIntermodalAccessEgressModes() {
		return parseModeList(restrictedIntermodalAccessEgressModes);
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
		comments.put(RESTRICT_VEHICLE_TO_HOME_ACTIVITY,
				"If true, restricted intermodal vehicle access is only allowed for trips starting at the configured activity type, and restricted vehicle egress only for trips ending at that activity type.");
		comments.put(VEHICLE_RESTRICTED_ACTIVITY_TYPE,
				"Activity type that allows restricted intermodal access from trip origin and restricted intermodal egress to trip destination.");
		comments.put(RESTRICTED_INTERMODAL_ACCESS_EGRESS_MODES,
				"Comma-separated intermodal access/egress modes to restrict to the configured activity type and track for routing-time vehicle continuity.");
		comments.put(ENFORCE_INTERMODAL_VEHICLE_CONTINUITY_DURING_ROUTING,
				"If true, routed PT candidates are constrained to retrieve an intermodal private vehicle at the same stop where it was left before returning home.");
		comments.put(INTERMODAL_VEHICLE_CONTINUITY_HOME_ACTIVITY_TYPE,
				"Destination activity type where an intermodal private vehicle must be retrieved if it was left at a PT stop earlier in the tour.");
		return comments;
	}

	static private String normalizeModeList(String value, String errorMessage) {
		Set<String> modes = parseModeList(value);
		if (modes.isEmpty()) {
			throw new IllegalArgumentException(errorMessage);
		}
		return String.join(",", modes);
	}

	static private Set<String> parseModeList(String value) {
		if (value == null || value.isBlank()) {
			return Collections.emptySet();
		}

		return Arrays.stream(value.split(",")) //
				.map(String::trim) //
				.filter(mode -> !mode.isEmpty()) //
				.collect(Collectors.toCollection(LinkedHashSet::new));
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
