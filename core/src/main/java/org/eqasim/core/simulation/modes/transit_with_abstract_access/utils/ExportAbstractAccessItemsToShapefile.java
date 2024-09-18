package org.eqasim.core.simulation.modes.transit_with_abstract_access.utils;


import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessesFileReader;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.geotools.api.feature.simple.SimpleFeature;

import java.util.Collection;
import java.util.LinkedList;

public class ExportAbstractAccessItemsToShapefile {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args).requireOptions("schedule-path", "items-path", "crs", "output-path").build();
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new TransitScheduleReader(scenario).readFile(commandLine.getOptionStrict("schedule-path"));
        AbstractAccessesFileReader reader = new AbstractAccessesFileReader(scenario.getTransitSchedule());
        reader.readFile(commandLine.getOptionStrict("items-path"));

        Collection<SimpleFeature> features = new LinkedList<>();

        CoordinateReferenceSystem crs = MGC.getCRS(commandLine.getOptionStrict("crs"));

        PointFeatureFactory pointFactory = new PointFeatureFactory.Builder() //
                .setCrs(crs).setName("id") //
                .addAttribute("id", String.class)
                .addAttribute("centerStop", String.class)//
                .addAttribute("radius", Double.class)
                .addAttribute("speed", Double.class)
                .addAttribute("type", String.class)
                .addAttribute("routed", Boolean.class)//
                .create();

        for(AbstractAccessItem item: reader.getAccessItems().values()) {
            SimpleFeature feature = pointFactory.createPoint(item.getCenterStop().getCoord(),
                    new Object[]{
                            item.getId().toString(),
                            item.getCenterStop().getId().toString(),
                            item.getRadius(),
                            item.getAvgSpeedToCenterStop(),
                            item.getAccessType(),
                            item.isUsingRoutedDistance()
                    }, null);
            features.add(feature);
        }
        ShapeFileWriter.writeGeometries(features, commandLine.getOptionStrict("output-path"));
    }
}
