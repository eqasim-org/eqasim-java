package org.eqasim.ile_de_france.vdf;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;

public class RunPrepareDemand {
    static public void main(String[] args) throws ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("input-path", "output-path", "sampling-rate", "seed")
                .build();

        String inputPath = cmd.getOptionStrict("input-path");
        String outputPath = cmd.getOptionStrict("output-path");
        double samplingRate = Double.parseDouble(cmd.getOptionStrict("sampling-rate"));
        int seed = Integer.parseInt(cmd.getOptionStrict("seed"));

        Random random = new Random(seed);

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        new PopulationReader(scenario).readFile(inputPath);

        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        Population tripPopulation = PopulationUtils.createPopulation(config);

        MainModeIdentifier identifier = TripStructureUtils.getRoutingModeIdentifier();

        int tripIndex = 0;

        for (Person person : population.getPersons().values()) {
            if (random.nextDouble() > samplingRate) {
                continue;
            }

            for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
                if (identifier.identifyMainMode(trip.getTripElements()).equals(TransportMode.car)) {
                    Person tripPerson = factory.createPerson(Id.createPersonId(tripIndex++));
                    tripPopulation.addPerson(tripPerson);

                    Plan tripPlan = factory.createPlan();
                    tripPerson.addPlan(tripPlan);

                    Activity originActivity = factory.createActivityFromLinkId("generic",
                            trip.getOriginActivity().getLinkId());
                    originActivity.setEndTime(trip.getOriginActivity().getEndTime().seconds());
                    tripPlan.addActivity(originActivity);

                    Leg leg = factory.createLeg(TransportMode.car);
                    tripPlan.addLeg(leg);

                    Activity destinationActivity = factory.createActivityFromLinkId("generic",
                            trip.getDestinationActivity().getLinkId());
                    tripPlan.addActivity(destinationActivity);
                }
            }
        }

        new PopulationWriter(tripPopulation).write(outputPath);
    }
}
