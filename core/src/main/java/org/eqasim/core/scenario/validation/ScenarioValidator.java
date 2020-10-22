package org.eqasim.core.scenario.validation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacility;

public class ScenarioValidator {
	private final static Logger logger = Logger.getLogger(ScenarioValidator.class);

	public boolean checkSpatialConsistency(Scenario scenario) {
		boolean errorsFound = false;

		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			if (facility.getLinkId() == null) {
				logger.error(String.format("Facility %s has no link", facility.getId().toString()));
				errorsFound = true;
			} else {
				Link link = scenario.getNetwork().getLinks().get(facility.getLinkId());

				if (link == null) {
					logger.error(String.format("Link %s of facility %s does not exist", facility.getLinkId().toString(),
							facility.getId().toString()));
					errorsFound = true;
				}
			}

			if (facility.getCoord() == null) {
				logger.error(String.format("Facility %s has no coordinate", facility.getId().toString()));
				errorsFound = true;
			}
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;

						boolean hasCoord = activity.getCoord() != null;
						boolean hasLink = activity.getLinkId() != null;
						boolean hasFacility = activity.getFacilityId() != null;

						if (!hasCoord) {
							logger.error(String.format("Person %s has %s activity without coordinate",
									person.getId().toString(), activity.getType()));
							errorsFound = true;
						}

						if (!hasLink) {
							logger.error(String.format("Person %s has %s activity without link",
									person.getId().toString(), activity.getType()));
							errorsFound = true;
						}

						if (!hasFacility && !TripStructureUtils.isStageActivityType(activity.getType())) {
							logger.error(String.format("Person %s has %s activity without facility",
									person.getId().toString(), activity.getType()));
							errorsFound = true;
						}

						if (hasCoord && hasFacility) {
							Coord activityCoord = activity.getCoord();
							Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());
							ActivityFacility facility = scenario.getActivityFacilities().getFacilities()
									.get(activity.getFacilityId());

							if (link == null) {
								logger.error(String.format("Link %s in %s activity for person %s does not exist",
										activity.getLinkId(), activity.getType(), person.getId().toString()));
								errorsFound = true;
							}

							if (facility == null) {
								logger.error(String.format("Facility %s in %s activity for person %s does not exist",
										activity.getFacilityId(), activity.getType(), person.getId().toString()));
								errorsFound = true;
							} else {
								Coord facilityCoord = facility.getCoord();

								if (!activityCoord.equals(facilityCoord)) {
									logger.error(String.format(
											"Facility %s and %s activity for person %s do not have same coordinates",
											activity.getFacilityId(), activity.getType(), person.getId().toString()));
									errorsFound = true;
								}
							}

							if (link != null && facility != null) {
								if (!link.getId().equals(facility.getLinkId())) {
									logger.error(String.format(
											"Facility %s and %s activity for person %s do not have same link",
											activity.getLinkId(), activity.getType(), person.getId().toString()));
									errorsFound = true;
								}
							}
						}

						if (!TripStructureUtils.isStageActivityType(activity.getType())) {
							Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());

							if (link != null) {
								if (!link.getAllowedModes().contains("car")) {
									logger.error(String.format("Person %s has %s activity attached to non-car link %s",
											person.getId().toString(), activity.getType(), link.getId().toString()));
									errorsFound = true;
								}
							}
						}
					}
				}
			}
		}

		return errorsFound;
	}

	public boolean checkRouting(Population population) {
		boolean errorsFound = false;

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (int i = 0; i < plan.getPlanElements().size(); i++) {
					PlanElement element = plan.getPlanElements().get(i);

					if (element instanceof Leg) {
						Leg leg = (Leg) element;

						if (leg.getRoute() == null) {
							logger.error(String.format("Person %s has %s leg without a route",
									person.getId().toString(), leg.getMode()));
							errorsFound = true;
						} else {
							Route route = leg.getRoute();

							if (route.getStartLinkId() == null) {
								logger.error(String.format("Person %s has route without a start link",
										person.getId().toString()));
								errorsFound = true;
							} else {
								Activity preceedingActivity = (Activity) plan.getPlanElements().get(i - 1);

								if (!TripStructureUtils.isStageActivityType(preceedingActivity.getType())) {
									if (!preceedingActivity.getLinkId().equals(route.getStartLinkId())) {
										logger.error(String.format(
												"Person %s has route with a different start link (%s) than previous activity (%s)",
												person.getId().toString(), route.getStartLinkId().toString(),
												preceedingActivity.getLinkId().toString()));
										errorsFound = true;
									}
								}
							}

							if (route.getEndLinkId() == null) {
								logger.error(String.format("Person %s has route without an end link",
										person.getId().toString()));
								errorsFound = true;
							} else {
								Activity followingActivity = (Activity) plan.getPlanElements().get(i - 1);

								if (!TripStructureUtils.isStageActivityType(followingActivity.getType())) {
									if (!followingActivity.getLinkId().equals(route.getStartLinkId())) {
										logger.error(String.format(
												"Person %s has route with a different end link (%s) than following activity (%s)",
												person.getId().toString(), route.getStartLinkId().toString(),
												followingActivity.getLinkId().toString()));
										errorsFound = true;
									}
								}
							}
						}
					}
				}
			}
		}

		return errorsFound;
	}

	public void checkScenario(Scenario scenario) {
		boolean errorsFound = false;

		errorsFound |= checkSpatialConsistency(scenario);
		errorsFound |= checkRouting(scenario.getPopulation());

		if (errorsFound) {
			throw new IllegalStateException("Found errors while checking population");
		} else {
			logger.info("Scenario is valid!");
		}
	}
}
