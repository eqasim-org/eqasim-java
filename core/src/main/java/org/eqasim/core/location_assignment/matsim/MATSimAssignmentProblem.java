package org.eqasim.core.location_assignment.matsim;

import java.util.List;
import java.util.Optional;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.matsim.utils.ActivityIndices;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

public class MATSimAssignmentProblem implements LocationAssignmentProblem {
	final private Optional<Activity> originActivity;
	final private Optional<Activity> destinationActivity;

	final private List<Activity> chainActivities;
	final private List<Leg> chainLegs;

	final private List<Activity> allActivities;
	final private List<Leg> allLegs;

	final private Optional<Vector2D> originLocation;
	final private Optional<Vector2D> destinationLocation;

	final private Plan plan;
	final private ActivityIndices activityIndices;

	public MATSimAssignmentProblem(Optional<Activity> originActivity, Optional<Activity> destinationActivity,
			List<Activity> allActivities, List<Leg> allLegs, List<Activity> chainActivities, List<Leg> chainLegs,
			Plan plan, ActivityIndices activityIndices) {
		this.originActivity = originActivity;
		this.destinationActivity = destinationActivity;
		this.chainActivities = chainActivities;
		this.chainLegs = chainLegs;
		this.allActivities = allActivities;
		this.allLegs = allLegs;
		this.plan = plan;
		this.activityIndices = activityIndices;

		this.originLocation = originActivity.isPresent()
				? Optional.of(
						new Vector2D(originActivity.get().getCoord().getX(), originActivity.get().getCoord().getY()))
				: Optional.empty();

		this.destinationLocation = destinationActivity.isPresent() ? Optional.of(
				new Vector2D(destinationActivity.get().getCoord().getX(), destinationActivity.get().getCoord().getY()))
				: Optional.empty();
	}

	static public MATSimAssignmentProblem create(Plan plan, ActivityIndices problemIndices) {
		List<PlanElement> planElements = plan.getPlanElements();

		Optional<Activity> originActivity = ActivityIndices.getOriginActivity(problemIndices, planElements);
		Optional<Activity> destinationActivity = ActivityIndices.getDestinationActivity(problemIndices, planElements);

		List<Activity> allActivities = ActivityIndices.getActivities(problemIndices, planElements, true);
		List<Activity> chainActivities = ActivityIndices.getActivities(problemIndices, planElements, false);

		List<Leg> allLegs = ActivityIndices.getLegs(problemIndices, planElements, true);
		List<Leg> chainLegs = ActivityIndices.getLegs(problemIndices, planElements, false);

		return new MATSimAssignmentProblem(originActivity, destinationActivity, allActivities, allLegs, chainActivities,
				chainLegs, plan, problemIndices);
	}

	public Optional<Activity> getOriginActivity() {
		return originActivity;
	}

	public Optional<Activity> getDestinationActivity() {
		return destinationActivity;
	}

	public List<Activity> getChainActivities() {
		return chainActivities;
	}

	public List<Leg> getChainLegs() {
		return chainLegs;
	}

	public List<Activity> getAllActivities() {
		return allActivities;
	}

	public List<Leg> getAllLegs() {
		return allLegs;
	}

	@Override
	public Optional<Vector2D> getOriginLocation() {
		return originLocation;
	}

	@Override
	public Optional<Vector2D> getDestinationLocation() {
		return destinationLocation;
	}

	public boolean isTailProblem() {
		return !(originActivity.isPresent() && destinationActivity.isPresent());
	}

	public Plan getPlan() {
		return plan;
	}

	public ActivityIndices getActivityIndices() {
		return activityIndices;
	}
}
