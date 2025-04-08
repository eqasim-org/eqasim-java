package org.eqasim.core.simulation.modes.parking_aware_car.utils;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AddActivityParkingAttributes {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("population-path", "network-path", "output-population-path")
                .allowOptions("activity-types", "parking-types")
                .build();

        String populationPath = commandLine.getOptionStrict("population-path");
        String networkPath = commandLine.getOptionStrict("network-path");
        String outputPopulationPath = commandLine.getOptionStrict("output-population-path");
        Set<String> activityTypes = commandLine.getOption("activity-types").map(s -> Set.of(s.split(","))).orElse(new HashSet<>());
        Set<Id<ParkingType>> parkingTypes = commandLine.getOption("parking-types").map(s -> Set.of(s.split(",")))
                .orElse(activityTypes).stream()
                .map(s -> Id.create(s, ParkingType.class))
                .collect(Collectors.toSet());

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new PopulationReader(scenario).readFile(populationPath);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);

        NetworkWideParkingSpaceStore networkWideParkingSpaceStore = new NetworkWideParkingSpaceStore(scenario.getNetwork());

        int requiredParkings = 0;
        int missedParkings = 0;

        for(Person person: scenario.getPopulation().getPersons().values()) {
            Map<String, IdSet<Link>> linksPerParkingType = new HashMap<>();
            for(PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
                if(!(planElement instanceof Activity activity)) {
                    continue;
                }
                if(!activityTypes.isEmpty() && !activityTypes.contains(activity.getType())) {
                    continue;
                }
                requiredParkings++;
                Set<Id<ParkingType>> potentialParkingTypes = parkingTypes;
                if(potentialParkingTypes.isEmpty()) {
                    potentialParkingTypes = Set.of(Id.create(activity.getType(), ParkingType.class));
                }
                boolean found = false;
                IdMap<ParkingType, ParkingSpace> potentialParkings = networkWideParkingSpaceStore.getLinkParkingSpaces(activity.getLinkId());
                for(Id<ParkingType> parkingTypeId: potentialParkings.keySet()) {
                    if(potentialParkingTypes.isEmpty() || potentialParkingTypes.contains(parkingTypeId)) {
                        found = true;
                        linksPerParkingType.computeIfAbsent(activity.getType(), type -> new IdSet<>(Link.class)).add(activity.getLinkId());
                    }
                }
                if(!found) {
                    missedParkings++;
                    System.out.println(String.format("No parking found on link %s as a %s activity location", activity.getLinkId().toString(), activity.getType()));
                }
            }
            Map<String, String> linkIdsPerParkingType = new HashMap<>();
            for(String key: linksPerParkingType.keySet()) {
                String linkIdsString = String.join(",", linksPerParkingType.get(key).stream().map(Object::toString).toList());
                linkIdsPerParkingType.put(key, linkIdsString);
            }
            if(linksPerParkingType.size() > 0) {
                person.getAttributes().putAttribute("parking", linkIdsPerParkingType);
            }
        }
        System.out.println(String.format("Missed %d/%d parkings", missedParkings, requiredParkings));
        new PopulationWriter(scenario.getPopulation()).write(outputPopulationPath);
    }
}
