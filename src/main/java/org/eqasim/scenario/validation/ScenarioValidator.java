package org.eqasim.scenario.validation;

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
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.ActivityFacility;

public class ScenarioValidator {
	private final static Logger logger = Logger.getLogger(ScenarioValidator.class);

	private final StageActivityTypes stageActivityTypes;

	public ScenarioValidator(StageActivityTypes stageActivityTypes) {
		this.stageActivityTypes = stageActivityTypes;
	}

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

						if (!hasFacility && !stageActivityTypes.isStageActivity(activity.getType())) {
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
										activity.getLinkId(), activity.getType(), person.getId().toString()));
								errorsFound = true;
							} else {
								Coord facilityCoord = facility.getCoord();

								if (!activityCoord.equals(facilityCoord)) {
									logger.error(String.format(
											"Facility %s and %s activity for person %s do not have same coordinates",
											activity.getLinkId(), activity.getType(), person.getId().toString()));
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
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;

						if (leg.getRoute() == null) {
							logger.error(String.format("Person %s has leg without a route", person.getId().toString()));
							errorsFound = true;
						} else {
							Route route = leg.getRoute();

							if (route.getStartLinkId() == null) {
								logger.error(String.format("Person %s has route without a start link",
										person.getId().toString()));
								errorsFound = true;
							}

							if (route.getEndLinkId() == null) {
								logger.error(String.format("Person %s has route without an end link",
										person.getId().toString()));
								errorsFound = true;
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
