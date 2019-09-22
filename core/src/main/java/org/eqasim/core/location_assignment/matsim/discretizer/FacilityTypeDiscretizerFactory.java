package org.eqasim.core.location_assignment.matsim.discretizer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

public class FacilityTypeDiscretizerFactory {
	final private Map<String, QuadTree<FacilityLocation>> indexAll = new HashMap<>();
	final private Map<String, QuadTree<FacilityLocation>> indexPtAccessible = new HashMap<>();
	final private Set<String> relevantActivityTypes = new HashSet<>();

	public FacilityTypeDiscretizerFactory(Set<String> relevantActivityTypes) {
		this.relevantActivityTypes.addAll(relevantActivityTypes);
	}

	public FacilityDiscretizer createDiscretizer(String activityType, boolean requirePtAccessible) {
		if (!indexAll.containsKey(activityType)) {
			throw new IllegalArgumentException(String.format("Activity type '%s' is not registered", activityType));
		}

		return new FacilityDiscretizer(requirePtAccessible ? indexPtAccessible.get(activityType) : indexAll.get(activityType));
	}

	public void loadFacilities(ActivityFacilities facilities) {
		Set<String> types = facilities.getFacilities().values().stream().map(f -> f.getActivityOptions().values())
				.flatMap(Collection::stream).map(ActivityOption::getType).collect(Collectors.toSet());
		types.retainAll(relevantActivityTypes);

		double minX = facilities.getFacilities().values().stream().map(ActivityFacility::getCoord)
				.mapToDouble(Coord::getX).min().getAsDouble();
		double maxX = facilities.getFacilities().values().stream().map(ActivityFacility::getCoord)
				.mapToDouble(Coord::getX).max().getAsDouble();
		double minY = facilities.getFacilities().values().stream().map(ActivityFacility::getCoord)
				.mapToDouble(Coord::getY).min().getAsDouble();
		double maxY = facilities.getFacilities().values().stream().map(ActivityFacility::getCoord)
				.mapToDouble(Coord::getY).max().getAsDouble();

		for (String type : types) {
			if (!indexAll.containsKey(type)) {
				indexAll.put(type, new QuadTree<FacilityLocation>(minX, minY, maxX, maxY));
			}
			
			if (!indexPtAccessible.containsKey(type)) {
				indexPtAccessible.put(type, new QuadTree<FacilityLocation>(minX, minY, maxX, maxY));
			}
		}

		for (ActivityFacility facility : facilities.getFacilities().values()) {
			FacilityLocation facilityLocation = new FacilityLocation(facility);
			Coord coord = facility.getCoord();

			for (ActivityOption option : facility.getActivityOptions().values()) {
				String activityType = option.getType();

				if (relevantActivityTypes.contains(activityType)) {
					indexAll.get(activityType).put(coord.getX(), coord.getY(), facilityLocation);
					
					Boolean ptAccessibleAttribute = (Boolean) facility.getAttributes().getAttribute("ptAccessible");
					
					if (ptAccessibleAttribute != null && ptAccessibleAttribute) {
						indexPtAccessible.get(activityType).put(coord.getX(), coord.getY(), facilityLocation);
					}
				}
			}
		}
	}
}
