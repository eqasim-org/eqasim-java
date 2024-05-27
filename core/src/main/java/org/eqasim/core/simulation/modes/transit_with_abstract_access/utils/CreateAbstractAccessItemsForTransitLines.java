package org.eqasim.core.simulation.modes.transit_with_abstract_access.utils;

import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessesFileReader;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessesFileWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.io.File;
import java.util.*;

public class CreateAbstractAccessItemsForTransitLines {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("transit-schedule-path", "output-path", "radius", "average-speed", "access-type", "use-routed-distance")
                .allowOptions("transit-lines-ids", "route-modes", "frequency", "append-to-output", "remove-redundant-items")
                .build();

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new TransitScheduleReader(scenario).readFile(commandLine.getOptionStrict("transit-schedule-path"));

        File outputFile = new File(commandLine.getOptionStrict("output-path"));

        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        Collection<AbstractAccessItem> items = new ArrayList<>();
        if(Boolean.parseBoolean(commandLine.getOption("append-to-output").orElse("false")) && outputFile.isFile()) {
            AbstractAccessesFileReader reader = new AbstractAccessesFileReader(transitSchedule);
            reader.readFile(outputFile.getAbsolutePath());
            items.addAll(reader.getAccessItems().values());
        }
        Set<TransitStopFacility> transitStopFacilities = new HashSet<>();
        if(commandLine.hasOption("transit-lines-ids")) {
            for(String idString: commandLine.getOptionStrict("transit-lines-ids").split(",")) {
                Id<TransitLine> transitLineId = Id.create(idString, TransitLine.class);
                TransitLine transitLine = transitSchedule.getTransitLines().get(transitLineId);
                if(transitLine == null) {
                    throw new IllegalStateException("Transit line " + idString + " does not exist");
                }
                for(TransitRoute transitRoute: transitLine.getRoutes().values()) {
                    for(TransitRouteStop transitRouteStop: transitRoute.getStops()) {
                        transitStopFacilities.add(transitRouteStop.getStopFacility());
                    }
                }
            }
        }
        if(commandLine.hasOption("route-modes")) {
            Collection<String> routeModes = Arrays.asList(commandLine.getOptionStrict("route-modes").split(","));
            transitSchedule.getTransitLines().values().stream()
                    .flatMap(transitLine -> transitLine.getRoutes().values().stream())
                    .filter(transitRoute -> routeModes.contains(transitRoute.getTransportMode()))
                    .flatMap(transitRoute -> transitRoute.getStops().stream())
                    .map(TransitRouteStop::getStopFacility)
                    .forEach(transitStopFacilities::add);
        }

        double radius = Double.parseDouble(commandLine.getOptionStrict("radius"));
        double avgSpeed = Double.parseDouble(commandLine.getOptionStrict("average-speed"));
        boolean usingRoutedDistance = Boolean.parseBoolean(commandLine.getOptionStrict("use-routed-distance"));
        int frequency = Integer.parseInt(commandLine.getOption("frequency").orElse("600"));

        String accessType = commandLine.getOptionStrict("access-type");

        HashSet<Id<AbstractAccessItem>> itemIds = new HashSet<>();
        for(AbstractAccessItem item: items) {
            itemIds.add(item.getId());
        }

        for(TransitStopFacility transitStopFacility: transitStopFacilities) {
            int id=-1;
            Id<AbstractAccessItem> itemId;
            do {
                id+=1;
                itemId = Id.create(transitStopFacility.getId().toString()+"-"+id, AbstractAccessItem.class);
            }while(itemIds.contains(itemId));
            AbstractAccessItem item = new AbstractAccessItem(itemId, transitStopFacility, radius, avgSpeed, accessType, usingRoutedDistance, frequency);
            items.add(item);
        }
        new AbstractAccessesFileWriter(items).write(outputFile.getAbsolutePath());
    }
}
