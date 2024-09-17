package org.eqasim.core.components.emissions;

import org.apache.commons.lang3.ArrayUtils;
import org.eqasim.core.misc.ClassUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.gis.ShapeFileReader;

public class RunComputeEmissionsGrid {

    public static void main(String[] args) throws CommandLine.ConfigurationException {

        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "domain-shp-path") //
                .allowOptions("scale-factor", "grid-size", "smooth-radius", "time-bin-size", "configurator-class")
                .build();
        
        EqasimConfigurator configurator;
        if(cmd.hasOption("configurator-class")) {
            configurator = ClassUtils.getInstanceOfClassExtendingOtherClass(cmd.getOptionStrict("configurator-class"), EqasimConfigurator.class);
        } else {
            configurator = new EqasimConfigurator();
        }

        ConfigGroup[] configGroups = ArrayUtils.addAll(configurator.getConfigGroups(), new EmissionsConfigGroup());

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configGroups);
        cmd.applyConfiguration(config);
        final String outputDirectory = config.controller().getOutputDirectory() + "/";

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(outputDirectory + "output_network.xml.gz");

        SimpleFeature analysisFeature = ShapeFileReader.getAllFeatures(cmd.getOptionStrict("domain-shp-path")).iterator().next();
        Geometry analysisGeometry = (Geometry) analysisFeature.getDefaultGeometry();

        double scaleFactor = Double.parseDouble(cmd.getOption("scale-factor").orElse("1.0"));
        int gridSize = Integer.parseInt(cmd.getOption("grid-size").orElse("25"));
        int smoothRadius = Integer.parseInt(cmd.getOption("smooth-radius").orElse("50"));
        int timeBinSize = Integer.parseInt(cmd.getOption("time-bin-size").orElse("3600"));

        new EmissionGridAnalyzer.Builder() //
                .withBounds(analysisGeometry) //
                .withNetwork(network) //
                .withCountScaleFactor(scaleFactor) //
                .withGridSize(gridSize) //
                .withSmoothingRadius(smoothRadius) //
                .withTimeBinSize(timeBinSize) //
                .withGridType(EmissionGridAnalyzer.GridType.Square) //
                .build() //
                .processToJsonFile(outputDirectory + "output_emissions_events.xml.gz", outputDirectory + "output_emissions.json");
    }

}
