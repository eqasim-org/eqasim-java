package org.eqasim.core.tools.sampling;

import java.util.Iterator;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

public class ScenarioSampler {
    private final Logger logger = LogManager.getLogger(ScenarioSampler.class);

    private final double samplingRate;
    private final Random random;

    public ScenarioSampler(double samplingRate, Random random) {
        this.samplingRate = samplingRate;
        this.random = random;
    }

    public void process(Scenario scenario) {
        // I) process persons
        Population population = scenario.getPopulation();

        IdSet<Person> removedPersons = new IdSet<>(Person.class);
        IdSet<Vehicle> removedVehicles = new IdSet<>(Vehicle.class);
        IdSet<ActivityFacility> removedFacilities = new IdSet<>(ActivityFacility.class);

        logger.info("Sampling persons ...");

        for (Person person : population.getPersons().values()) {
            if (random.nextDouble() > samplingRate) {
                removedPersons.add(person.getId());
                removedVehicles.addAll(VehicleUtils.getVehicleIds(person).values());

                for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                    if (element instanceof Activity acitvity && acitvity.getType().equals("home")) {
                        removedFacilities.add(acitvity.getFacilityId());
                        break;
                    }
                }
            }
        }

        logger.info("  removed " + removedPersons.size() + " persons.");

        removedPersons.forEach(population::removePerson);

        // II) process households
        logger.info("Cleaning households ...");

        Iterator<Household> householdIterator = scenario.getHouseholds().getHouseholds().values().iterator();
        int removedHouseholds = 0;

        while (householdIterator.hasNext()) {
            Household household = householdIterator.next();
            household.getMemberIds().removeAll(removedPersons);

            if (household.getMemberIds().size() == 0) {
                householdIterator.remove();
                removedVehicles.addAll(household.getVehicleIds());
                removedHouseholds++;
            }
        }

        logger.info("  removed " + removedHouseholds + " households.");

        // III) process vehicles
        logger.info("Cleaning vehicles ...");

        for (Person person : population.getPersons().values()) {
            // keep those that are still assigned
            removedVehicles.removeAll(VehicleUtils.getVehicleIds(person).values());
        }

        for (Household household : scenario.getHouseholds().getHouseholds().values()) {
            // keep those that are still assigned
            removedVehicles.removeAll(household.getVehicleIds());
        }

        removedVehicles.forEach(scenario.getVehicles().getVehicles()::remove);
        logger.info("  removed " + removedVehicles.size() + " vehicles.");

        // IV) process facilities
        logger.info("Cleaning home facilities ...");

        for (Person person : population.getPersons().values()) {
            // keep those that are still assigned
            for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
                if (element instanceof Activity acitvity && acitvity.getType().equals("home")) {
                    removedFacilities.remove(acitvity.getFacilityId());
                    break;
                }
            }
        }

        removedFacilities.forEach(scenario.getActivityFacilities().getFacilities()::remove);
        logger.info("  removed " + removedFacilities.size() + " home facilities.");
    }
}
