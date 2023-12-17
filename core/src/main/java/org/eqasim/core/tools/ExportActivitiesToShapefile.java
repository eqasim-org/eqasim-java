package org.eqasim.core.tools;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.*;
import java.util.stream.Collectors;

public class ExportActivitiesToShapefile {

    public static void exportActivitiesToShapeFile(Population population, String crsString, Set<String> ignoredActivityTypesSet, String outputPath) {

        CoordinateReferenceSystem crs = MGC.getCRS(crsString);

        PointFeatureFactory pointFactory = new PointFeatureFactory.Builder() //
                .setCrs(crs).setName("id") //
                .addAttribute("personId", String.class)
                .addAttribute("activityIndex", Integer.class)
                .addAttribute("type", String.class)//
                .addAttribute("linkId", String.class)
                .addAttribute("facilityId", String.class)
                .addAttribute("startTime", Double.class)
                .addAttribute("endTime", Double.class)//
                .create();

        Collection<SimpleFeature> features = new LinkedList<>();

        for(Person person: population.getPersons().values()) {
            if(person.getSelectedPlan() == null) {
                continue;
            }
            int activityIndex = -1;
            for(PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
                if (!(planElement instanceof Activity)) {
                    continue;
                }
                Activity a = (Activity) planElement;
                activityIndex++;
                if(ignoredActivityTypesSet.contains(a.getType())) {
                    continue;
                }
                Coordinate coordinate = new Coordinate(a.getCoord().getX(), a.getCoord().getY());
                SimpleFeature feature = pointFactory.createPoint(coordinate,
                    new Object[] {
                            person.getId().toString(),
                            activityIndex,
                            a.getType(),
                            a.getLinkId().toString(),
                            a.getFacilityId() == null ? null : a.getFacilityId().toString(),
                            a.getStartTime().orElse(Double.NaN),
                            a.getEndTime().orElse(Double.NaN)
                    },
                    null);
                features.add(feature);
            }
        }
        ShapeFileWriter.writeGeometries(features, outputPath);
    }


    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args).requireOptions("plans-path", "output-path", "crs")
                .allowOptions("ignored-activity-types").build();

        String plansPath = commandLine.getOptionStrict("plans-path");
        String outputPath = commandLine.getOptionStrict("output-path");
        String crs = commandLine.getOptionStrict("crs");
        Set<String> ignoredActivityTypes = Arrays.stream(commandLine.getOption("ignored-activity-types").orElse("").split(","))
                .map(String::trim)
                .filter(s -> s.length()>0)
                .collect(Collectors.toSet());

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(plansPath);

        exportActivitiesToShapeFile(scenario.getPopulation(), crs, ignoredActivityTypes, outputPath);
    }
}
