package org.eqasim;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.analysis.run.RunLegAnalysis;
import org.eqasim.core.analysis.run.RunPublicTransportLegAnalysis;
import org.eqasim.core.analysis.run.RunTripAnalysis;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.tools.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class TestSimulationPipeline {
    
    @Before
    public void setUp() throws IOException {
        URL fixtureUrl = getClass().getClassLoader().getResource("melun");
        FileUtils.copyDirectory(new File(fixtureUrl.getPath()), new File("melun_test/input"));
        FileUtils.forceMkdir(new File("melun_test/exports"));
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File("melun_test"));
    }

    private void runMelunSimulation() {
        EqasimConfigurator eqasimConfigurator = new EqasimConfigurator();
        Config config = ConfigUtils.loadConfig("melun_test/input/config.xml", eqasimConfigurator.getConfigGroups());
        ((ControlerConfigGroup) config.getModules().get(ControlerConfigGroup.GROUP_NAME)).setOutputDirectory("melun_test/output");

        Scenario scenario = ScenarioUtils.createScenario(config);
        eqasimConfigurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        eqasimConfigurator.adjustScenario(scenario);


        Controler controller = new Controler(scenario);
        eqasimConfigurator.configureController(controller);
        controller.addOverridingModule(new EqasimModeChoiceModule());
        controller.addOverridingModule(new EqasimAnalysisModule());
        controller.addOverridingModule(new AbstractEqasimExtension() {
            @Override
            protected void installEqasimExtension() {
                bind(ModeParameters.class);
                bindModeAvailability("DefaultModeAvailability").toProvider(() -> (person, trips) -> {
                    Set<String> modes = new HashSet<>();
                    modes.add(TransportMode.walk);
                    modes.add(TransportMode.pt);
                    modes.add(TransportMode.car);
                    modes.add(TransportMode.bike);
                    // Add special mode "car_passenger" if applicable
                    Boolean isCarPassenger = (Boolean) person.getAttributes().getAttribute("isPassenger");
                    if(isCarPassenger) {
                        modes.add("car_passenger");
                    }
                    return modes;
                }).asEagerSingleton();
            }
        });
        controller.run();
    }

    private void runAnalyses() throws CommandLine.ConfigurationException, IOException {
        RunTripAnalysis.main(new String[] {
                "--events-path", "melun_test/output/output_events.xml.gz",
                "--network-path", "melun_test/input/network.xml.gz",
                "--output-path", "melun_test/output/eqasim_trips_post_sim.csv"
        });

        assert CRCChecksum.getCRCFromFile("melun_test/output/eqasim_trips.csv") == CRCChecksum.getCRCFromFile("melun_test/output/eqasim_trips_post_sim.csv");

        RunLegAnalysis.main(new String[]{
                "--events-path", "melun_test/output/output_events.xml.gz",
                "--network-path", "melun_test/input/network.xml.gz",
                "--output-path", "melun_test/output/eqasim_legs_post_sim.csv"

        });

        assert CRCChecksum.getCRCFromFile("melun_test/output/eqasim_legs.csv") == CRCChecksum.getCRCFromFile("melun_test/output/eqasim_legs_post_sim.csv");

        RunPublicTransportLegAnalysis.main(new String[] {
                "--events-path", "melun_test/output/output_events.xml.gz",
                "--schedule-path", "melun_test/input/transit_schedule.xml.gz",
                "--output-path", "melun_test/output/eqasim_pt_post_sim.csv"
        });

        assert CRCChecksum.getCRCFromFile("melun_test/output/eqasim_pt.csv") == CRCChecksum.getCRCFromFile("melun_test/output/eqasim_pt_post_sim.csv");
    }

    private void runExports() throws Exception {
        ExportTransitLinesToShapefile.main(new String[] {
                "--schedule-path", "melun_test/input/transit_schedule.xml.gz",
                "--network-path", "melun_test/input/network.xml.gz",
                "--crs", "EPSG:2154",
                "--output-path", "melun_test/exports/lines.shp"
        });

        ExportTransitLinesToShapefile.main(new String[] {
                "--schedule-path", "melun_test/input/transit_schedule.xml.gz",
                "--network-path", "melun_test/input/network.xml.gz",
                "--crs", "EPSG:2154",
                "--modes", "rail",
                "--output-path", "melun_test/exports/lines_rail.shp"
        });

        ExportTransitLinesToShapefile.main(new String[] {
                "--schedule-path", "melun_test/input/transit_schedule.xml.gz",
                "--network-path", "melun_test/input/network.xml.gz",
                "--crs", "EPSG:2154",
                "--transit-lines", "IDFM:C02364,IDFM:C00879",
                "--output-path", "melun_test/exports/lines_line_ids.shp"
        });

        ExportTransitLinesToShapefile.main(new String[] {
                "--schedule-path", "melun_test/input/transit_schedule.xml.gz",
                "--network-path", "melun_test/input/network.xml.gz",
                "--crs", "EPSG:2154",
                "--transit-routes", "IDFM:TRANSDEV_AMV:27719-C00637-14017001,IDFM:SNCF:42048-C01728-9e8c577f-7ff9-4fe7-93e7-3c3854aa5ecf",
                "--output-path", "melun_test/exports/lines_route_ids.shp"
        });

        ExportTransitStopsToShapefile.main(new String[] {
                "--schedule-path", "melun_test/input/transit_schedule.xml.gz",
                "--crs", "EPSG:2154",
                "--output-path", "melun_test/exports/stops.shp"
        });

        ExportNetworkToShapefile.main(new String[] {
                "--network-path", "melun_test/input/network.xml.gz",
                "--crs", "EPSG:2154",
                "--output-path", "melun_test/exports/network.shp"
        });

        ExportActivitiesToShapefile.main(new String[]{
                "--plans-path", "melun_test/input/population.xml.gz",
                "--output-path", "melun_test/exports/activities.shp",
                "--crs", "EPSG:2154"
        });

        ExportPopulationToCSV.main(new String[]{
                "--plans-path", "melun_test/input/population.xml.gz",
                "--output-path", "melun_test/exports/persons.csv"
        });
    }

    @Test
    public void testPipeline() throws Exception {
        runMelunSimulation();
        runAnalyses();
        runExports();
    }
}
