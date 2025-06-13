package org.eqasim.core.scenario.freeflow;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FreeflowConfiguration {
	@JsonProperty("major_factor")
	public double majorFactor = 1.0;

	@JsonProperty("intermediate_factor")
	public double intermediateFactor = 1.0;

	@JsonProperty("minor_factor")
	public double minorFactor = 1.0;

	@JsonProperty("major_crossing_penalty_s")
	public double majorCrossingPenalty_s = 0.0;

	@JsonProperty("equal_crossing_penalty_s")
	public double equalCrossingPenalty_s = 0.0;

	@JsonProperty("minor_crossing_penalty_s")
	public double minorCrossingPenalty_s = 0.0;

	@JsonProperty("areas")
	public List<Area> areas = new LinkedList<>();

	static public class Area {
		@JsonProperty("wkt")
		String wkt = "";

		@JsonProperty("factor")
		double factor = 1.0;

		@JsonProperty("name")
		String name = "";
	}
}
