package org.eqasim.ile_de_france;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPredictorUtils;
import org.eqasim.ile_de_france.parking.ParkingPressure;
import org.eqasim.ile_de_france.parking.ParkingTariff;
import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.base.Preconditions;

public class RunTemporaryModelModification {
    private final static Logger logger = LogManager.getLogger(RunTemporaryModelModification.class);
    private final static GeometryFactory geometryFactory = new GeometryFactory();

    static public void main(String[] args) throws ConfigurationException, IOException, InterruptedException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "data-path", "output-population-path", "output-network-path") //
                .build();

        IDFConfigurator configurator = new IDFConfigurator(cmd);
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        configurator.updateConfig(config);
        cmd.applyConfiguration(config);

        logger.info("Reading zone information ...");
        List<Item> items = new LinkedList<>();
        try (GeoPackage gpkg = new GeoPackage(new File(cmd.getOptionStrict("data-path")))) {
            FeatureEntry featureEntry = gpkg.feature("data");
            SimpleFeatureReader reader = gpkg.reader(featureEntry, null, null);

            while (reader.hasNext()) {
                SimpleFeature feature = reader.next();

                String municipalityId = (String) feature.getAttribute("municipality_id");
                Preconditions.checkNotNull(municipalityId);

                Double parkingPressure = (Double) feature.getAttribute("parking_pressure");
                Preconditions.checkNotNull(parkingPressure);

                Double parkingTariff = (Double) feature.getAttribute("parking_tariff");
                Preconditions.checkNotNull(parkingTariff);

                items.add(new Item(municipalityId, parkingPressure, parkingTariff,
                        (Geometry) feature.getDefaultGeometry()));
            }
        }

        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.adjustScenario(scenario);

        new PopulationReader(scenario)
                .readURL(config.plans().getInputFileURL(config.getContext()));

        new MatsimNetworkReader(scenario.getNetwork()).readURL(config.network().getInputFileURL(config.getContext()));

        ParallelProgress progress = new ParallelProgress("Processing population ...",
                scenario.getPopulation().getPersons().size());
        progress.start();

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                Activity homeActivity = null;

                for (Activity activity : TripStructureUtils.getActivities(plan,
                        StageActivityHandling.ExcludeStageActivities)) {
                    if (homeActivity == null && activity.getType().equals("home")) {
                        homeActivity = activity;
                    }

                    Coordinate coordinate = new Coordinate(activity.getCoord().getX(), activity.getCoord().getY());
                    Point pointGeometry = geometryFactory.createPoint(coordinate);

                    for (Item item : items) {
                        if (item.geometry.covers(pointGeometry)) {
                            activity.getAttributes().putAttribute(IDFPredictorUtils.ACTIVITY_MUNICIPALITY_ID,
                                    item.municipalityId);
                            break;
                        }
                    }
                }

                if (homeActivity != null) {
                    person.getAttributes().putAttribute(IDFPredictorUtils.RESIDENCE_MUNICIPALITY_ID,
                            homeActivity.getAttributes().getAttribute(IDFPredictorUtils.ACTIVITY_MUNICIPALITY_ID));
                }
            }

            progress.update();
        }

        progress.close();

        progress = new ParallelProgress("Processing network ...",
                scenario.getNetwork().getLinks().size());
        progress.start();

        for (Link link : scenario.getNetwork().getLinks().values()) {
            Coordinate coordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
            Point pointGeometry = geometryFactory.createPoint(coordinate);

            for (Item item : items) {
                if (item.geometry.covers(pointGeometry)) {
                    link.getAttributes().putAttribute(ParkingPressure.LINK_ATTRIBUTE, item.parkingPressure);
                    link.getAttributes().putAttribute(ParkingTariff.LINK_ATTRIBUTE, item.parkingTariff);
                    break;
                }
            }

            progress.update();
        }

        progress.close();

        new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-population-path"));
        new NetworkWriter(scenario.getNetwork()).write(cmd.getOptionStrict("output-network-path"));
    }

    private record Item(String municipalityId, double parkingPressure, double parkingTariff, Geometry geometry) {
    }
}
