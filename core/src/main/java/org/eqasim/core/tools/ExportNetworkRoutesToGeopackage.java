package org.eqasim.core.tools;

import org.eqasim.core.misc.ClassUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ExportNetworkRoutesToGeopackage {


    private static Collection<SimpleFeature> extractFeaturesFromPerson(Person person, Network network, SimpleFeatureBuilder featureBuilder, GeometryFactory geometryFactory, Set<String> mainModes, Set<String> legModes) {
        List<SimpleFeature> features = new ArrayList<>();
        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan());
        for(int tripIndex=0; tripIndex<trips.size(); tripIndex++) {
            TripStructureUtils.Trip trip = trips.get(tripIndex);
            List<Leg> tripLegs = trip.getLegsOnly();
            if(tripLegs.size() == 0) {
                continue;
            }
            if(mainModes.size() > 0 && !mainModes.contains(tripLegs.get(0).getRoutingMode())) {
                continue;
            }
            for(int legIndex=0; legIndex<tripLegs.size(); legIndex++) {
                Leg leg = tripLegs.get(legIndex);
                if(legModes.size() > 0 && !legModes.contains(leg.getMode())) {
                    continue;
                }
                Route route = leg.getRoute();
                if(!(route instanceof NetworkRoute networkRoute)) {
                    continue;
                }
                List<Id<Link>> linkIds = networkRoute.getLinkIds();
                if(linkIds.size() == 0) {
                    continue;
                }
                Coordinate[] coordinates = new Coordinate[linkIds.size() + 1];


                for(int linkIndex=0; linkIndex<linkIds.size(); linkIndex++) {
                    Link link = network.getLinks().get(linkIds.get(linkIndex));

                    if (linkIndex == 0) {
                        coordinates[linkIndex] = new Coordinate(link.getFromNode().getCoord().getX(),
                                link.getFromNode().getCoord().getY());
                    }

                    coordinates[linkIndex + 1] = new Coordinate(link.getToNode().getCoord().getX(),
                            link.getToNode().getCoord().getY());
                }

                featureBuilder.add(person.getId().toString());
                featureBuilder.add(tripIndex);
                featureBuilder.add(leg.getRoutingMode());
                featureBuilder.add(legIndex);
                featureBuilder.add(leg.getMode());



                featureBuilder.add(geometryFactory.createLineString(coordinates));
                features.add(featureBuilder.buildFeature(null));
            }
        }
        return features;
    }

    public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("network-path", "plans-path", "output-path", "crs")
                .allowOptions("configurator-class")
                .allowOptions("main-modes", "leg-modes")
                .build();

        String networkPath = commandLine.getOptionStrict("network-path");
        String populationPath = commandLine.getOptionStrict("plans-path");
        File outputPath = new File(commandLine.getOptionStrict("output-path"));
        CoordinateReferenceSystem crs = MGC.getCRS(commandLine.getOptionStrict("crs"));
        Set<String> mainModes = commandLine.hasOption("main-modes") ? Arrays.stream(commandLine.getOptionStrict("main-modes").split(",")).map(String::trim).collect(Collectors.toSet()) : new HashSet<>();
        Set<String> legModes = commandLine.hasOption("leg-modes") ? Arrays.stream(commandLine.getOptionStrict("leg-modes").split(",")).map(String::trim).collect(Collectors.toSet()) : new HashSet<>();

        EqasimConfigurator configurator = commandLine.hasOption("configurator-class") ? ClassUtils.getInstanceOfClassExtendingOtherClass(commandLine.getOptionStrict("configurator"), EqasimConfigurator.class) : new EqasimConfigurator();

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        configurator.configureScenario(scenario);
        new PopulationReader(scenario).readFile(populationPath);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);
        configurator.adjustScenario(scenario);


        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();

        featureTypeBuilder.setName("network");
        featureTypeBuilder.setCRS(crs);
        featureTypeBuilder.setDefaultGeometry("geometry");

        featureTypeBuilder.add("person_id", String.class);
        featureTypeBuilder.add("trip_index", Integer.class);
        featureTypeBuilder.add("trip_mode", String.class);
        featureTypeBuilder.add("leg_index", Integer.class);
        featureTypeBuilder.add("leg_mode", String.class);
        featureTypeBuilder.add("geometry", LineString.class);

        SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        GeometryFactory geometryFactory = new GeometryFactory();

        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();

        scenario.getPopulation().getPersons().values().stream().flatMap(person -> extractFeaturesFromPerson(person, scenario.getNetwork(), featureBuilder, geometryFactory, mainModes, legModes).stream()).forEach(featureCollection::add);

        if(outputPath.exists()) {
            outputPath.delete();
        }

        GeoPackage outputPackage = new GeoPackage(outputPath);
        outputPackage.init();

        FeatureEntry featureEntry = new FeatureEntry();
        outputPackage.add(featureEntry, featureCollection);

        outputPackage.close();
    }
}
