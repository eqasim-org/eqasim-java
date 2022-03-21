package org.eqasim.ile_de_france.emissions;

import org.apache.commons.lang3.ArrayUtils;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

// TODO: will need to be updated after the matsim 14 release to profit from https://github.com/matsim-org/matsim-libs/pull/1859

public class RunExportEmissionsNetwork {

    public static void main(String[] args) throws CommandLine.ConfigurationException {

        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path") //
                .allowOptions("time-bin-size")
                .build();

        ConfigGroup[] configGroups = ArrayUtils.addAll(new IDFConfigurator().getConfigGroups(), new EmissionsConfigGroup());

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configGroups);
        cmd.applyConfiguration(config);
        final String outputDirectory = config.controler().getOutputDirectory() + "/";

        int timeBinSize = Integer.parseInt(cmd.getOption("time-bin-size").orElse("3600"));

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionsOnLinkEventHandler handler = new EmissionsOnLinkEventHandler(timeBinSize);

        EmissionEventsReader eventsReader = new EmissionEventsReader(eventsManager);

        eventsManager.addHandler(handler);
        eventsManager.initProcessing();
        eventsReader.readFile(outputDirectory + "output_emissions_events.xml.gz");
        eventsManager.finishProcessing();

        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(outputDirectory + "output_network.xml.gz");
        Map<Id<Link>, ? extends Link> links = network.getLinks();
        TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> res = handler.getTimeBins();
        Collection<SimpleFeature> features = new LinkedList<>();

        PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder() //
                .setCrs(MGC.getCRS("epsg:2154")).setName("Emissions") //
                .addAttribute("link", String.class) //
                .addAttribute("time", Integer.class) //
                .addAttribute("PM", Double.class) //
                .addAttribute("FC", Double.class) //
                .addAttribute("CO", Double.class) //
                .addAttribute("FC_MJ", Double.class) //
                .addAttribute("HC", Double.class) //
                .addAttribute("NOx", Double.class) //
                .addAttribute("CO2_rep", Double.class) //
                .create();

        for (TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> timeBin : res.getTimeBins()) {
            int startTime = (int) timeBin.getStartTime();
            Map<Id<Link>, EmissionsByPollutant> map = timeBin.getValue();
            for (Map.Entry<Id<Link>, EmissionsByPollutant> entry : map.entrySet()) {
                Id<Link> link_id = entry.getKey();
                Link link = links.get(link_id);
                Coordinate fromCoordinate = new Coordinate(link.getFromNode().getCoord().getX(),
                        link.getFromNode().getCoord().getY());
                Coordinate toCoordinate = new Coordinate(link.getToNode().getCoord().getX(),
                        link.getToNode().getCoord().getY());

                List<Object> attributes = new ArrayList<>();

                attributes.add(link_id.toString());
                attributes.add(startTime);
                EmissionsByPollutant emissions = entry.getValue();
                Map<Pollutant, Double> pollutants = emissions.getEmissions();
                for (Map.Entry<Pollutant, Double> pollutant : pollutants.entrySet()) {
                    attributes.add(pollutant.getValue());
                }

                SimpleFeature feature = linkFactory.createPolyline( //
                        new Coordinate[] { fromCoordinate, toCoordinate }, //
                        attributes.toArray(),
                        null);

                features.add(feature);
            }
        }
        ShapeFileWriter.writeGeometries(features, outputDirectory + "emissions_network.shp");
    }
}
