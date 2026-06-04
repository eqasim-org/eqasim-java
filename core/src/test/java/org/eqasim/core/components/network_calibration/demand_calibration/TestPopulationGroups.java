package org.eqasim.core.components.network_calibration.demand_calibration;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class TestPopulationGroups {
    @Test
    public void testStartsWithInitialCellGrid() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Person first = addPerson(scenario, "first", 1_000.0, 1_000.0);
        Person second = addPerson(scenario, "second", 15_000.0, 1_000.0);

        PopulationGroups groups = PopulationGroups.build(scenario);

        assertNotEquals(groups.getGroup(first), groups.getGroup(second));
    }

    @Test
    public void testKeepsSplittingOverloadedCells() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Person clustered = null;

        for (int index = 0; index < 6_000; index++) {
            clustered = addPerson(scenario, "clustered_" + index, 100.0, 100.0);
        }

        Person nearby = addPerson(scenario, "nearby", 2_400.0, 100.0);

        PopulationGroups groups = PopulationGroups.build(scenario);

        assertNotEquals(groups.getGroup(clustered), groups.getGroup(nearby));
    }

    private static Person addPerson(Scenario scenario, String id, double x, double y) {
        PopulationFactory factory = scenario.getPopulation().getFactory();
        Person person = factory.createPerson(Id.createPersonId(id));

        person.getAttributes().putAttribute("home_x", x);
        person.getAttributes().putAttribute("home_y", y);

        scenario.getPopulation().addPerson(person);
        return person;
    }
}

