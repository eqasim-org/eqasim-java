package org.sutlab.seville_extended.mode_choice;

import com.google.inject.Inject;

// Requests the zones at startup, so that a missing shape fails before the first iteration
public class SevilleZonesLoader {
	@Inject
	public SevilleZonesLoader(SevilleZones zones) {
	}
}
