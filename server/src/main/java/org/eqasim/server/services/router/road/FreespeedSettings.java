package org.eqasim.server.services.router.road;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FreespeedSettings {
	@JsonProperty("major_factor")
	public double majorFactor = 1.0;

	@JsonProperty("intermediate_factor")
	public double intermediateFactor = 1.0;

	@JsonProperty("minor_factor")
	public double minorFactor = 1.0;

	@JsonProperty("major_crossing_penalty_s")
	public double majorCrossingPenalty_s = 1.0;

	@JsonProperty("minor_crossing_penalty_s")
	public double minorCrossingPenalty_s = 1.0;
}
