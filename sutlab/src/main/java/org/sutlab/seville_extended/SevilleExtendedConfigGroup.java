package org.sutlab.seville_extended;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class SevilleExtendedConfigGroup extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "sevilleExtended";

	static private final String MUNICIPALITY_PATH = "municipalityPath";
	static private final String PARKING_ZONE_PATH = "parkingZonePath";

	private String municipalityPath = null;
	private String parkingZonePath = null;

	public SevilleExtendedConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MUNICIPALITY_PATH, "Shapefile or GeoPackage with the boundary of the municipality of Sevilla (one polygon). "
				+ "Required when sevillePt.betaOutsideBoundary_u is not zero.");
		map.put(PARKING_ZONE_PATH, "Shapefile or GeoPackage with the parking zone Casco Antiguo (one polygon). "
				+ "Required when sevilleCar.betaParkingZone_u is not zero.");
		return map;
	}

	@StringGetter(MUNICIPALITY_PATH)
	public String getMunicipalityPath() {
		return municipalityPath;
	}

	@StringSetter(MUNICIPALITY_PATH)
	public void setMunicipalityPath(String municipalityPath) {
		this.municipalityPath = municipalityPath;
	}

	@StringGetter(PARKING_ZONE_PATH)
	public String getParkingZonePath() {
		return parkingZonePath;
	}

	@StringSetter(PARKING_ZONE_PATH)
	public void setParkingZonePath(String parkingZonePath) {
		this.parkingZonePath = parkingZonePath;
	}

	static public SevilleExtendedConfigGroup getOrCreate(Config config) {
		SevilleExtendedConfigGroup group = (SevilleExtendedConfigGroup) config.getModules().get(GROUP_NAME);

		if (group == null) {
			group = new SevilleExtendedConfigGroup();
			config.addModule(group);
		}

		return group;
	}
}
