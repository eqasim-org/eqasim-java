package org.eqasim.core.misc;

import org.matsim.core.router.StageActivityTypes;

public class InteractionStageActivityTypes implements StageActivityTypes {
	@Override
	public boolean isStageActivity(String activityType) {
		return activityType.endsWith(" interaction");
	}
}
