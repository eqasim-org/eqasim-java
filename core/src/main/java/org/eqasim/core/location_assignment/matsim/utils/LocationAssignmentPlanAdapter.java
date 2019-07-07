package org.eqasim.core.location_assignment.matsim.utils;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.assignment.LocationAssignmentResult;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocation;
import org.eqasim.core.location_assignment.matsim.MATSimAssignmentProblem;
import org.eqasim.core.location_assignment.matsim.discretizer.FacilityLocation;
import org.eqasim.core.location_assignment.matsim.solver.MATSimSolverResult;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.facilities.ActivityFacility;

public class LocationAssignmentPlanAdapter
		implements Function<MATSimSolverResult, MATSimSolverResult>, Consumer<MATSimSolverResult> {
	@Override
	public void accept(MATSimSolverResult result_) {
		MATSimAssignmentProblem problem = result_.getProblem();
		LocationAssignmentResult result = result_.getResult();

		problem.getAllLegs().forEach(leg -> leg.setRoute(null));

		for (int i = 0; i < problem.getChainActivities().size(); i++) {
			Activity activity = problem.getChainActivities().get(i);
			DiscreteLocation discreteLocation = result.getDiscreteLocationResult().getDiscreteLocations().get(i);

			if (discreteLocation instanceof FacilityLocation) {
				ActivityFacility facility = ((FacilityLocation) discreteLocation).getFacility();
				activity.setCoord(facility.getCoord());
				activity.setLinkId(facility.getLinkId());
				activity.setFacilityId(facility.getId());
			} else {
				Vector2D coord = discreteLocation.getLocation();
				activity.setCoord(new Coord(coord.getX(), coord.getY()));
				activity.setLinkId(null);
				activity.setFacilityId(null);
			}
		}
	}

	@Override
	public MATSimSolverResult apply(MATSimSolverResult result) {
		accept(result);
		return result;
	}
}
