package org.eqasim.ile_de_france;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.ile_de_france.emissions.RunComputeEmissionsEvents;
import org.eqasim.ile_de_france.emissions.RunExportEmissionsNetwork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.examples.ExamplesUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.*;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class TestEmissions {

    @Before
    public void setUp() throws IOException {
        URL fixtureUrl = getClass().getResource("/melun");
        FileUtils.copyDirectory(new File(fixtureUrl.getPath()), new File("melun_test/input"));
        var coldAverageFile = "sample_41_EFA_ColdStart_vehcat_2020average.csv";
        var coldDetailedFile = "sample_41_EFA_ColdStart_SubSegm_2020detailed.csv";
        var hotAverageFile = "sample_41_EFA_HOT_vehcat_2020average.csv";
        var hotDetailedFile = "sample_41_EFA_HOT_SubSegm_2020detailed.csv";
        for (String file: new String[]{ coldAverageFile, coldDetailedFile, hotAverageFile, hotDetailedFile}) {
            Files.copy(ExamplesUtils.class.getResourceAsStream("/test/scenarios/emissions-sampleScenario/" + file),
                    Paths.get("melun_test/input/", file),
                    REPLACE_EXISTING);
        }
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File("melun_test"));
    }

    private void runMelunSimulation() {
        EqasimConfigurator eqasimConfigurator = new EqasimConfigurator();
        Config config = ConfigUtils.loadConfig("melun_test/input/config_emissions.xml", eqasimConfigurator.getConfigGroups());
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

    private void runCreateVehicles() {
        VehicleType testCarType = VehicleUtils.createVehicleType(Id.create("test_car", VehicleType.class));
        testCarType.setLength(7.5);
        testCarType.setWidth(1.);
        testCarType.setNetworkMode("car");
        Attributes hbefa_attributes = testCarType.getEngineInformation().getAttributes();
        hbefa_attributes.putAttribute("HbefaVehicleCategory", "PASSENGER_CAR");
        hbefa_attributes.putAttribute("HbefaTechnology", "diesel");
        hbefa_attributes.putAttribute("HbefaSizeClass", "&lt;1,4L");
        hbefa_attributes.putAttribute("HbefaEmissionsConcept", "PC diesel Euro-3 (DPF)");

        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        vehicles.addVehicleType(testCarType);

        EqasimConfigurator eqasimConfigurator = new EqasimConfigurator();
        Config config = ConfigUtils.loadConfig("melun_test/input/config.xml", eqasimConfigurator.getConfigGroups());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        for (Person person: scenario.getPopulation().getPersons().values()) {
            Vehicle vehicle = VehicleUtils.createVehicle(
                    Id.createVehicleId(person.getId().toString()),
                    testCarType);
            vehicles.addVehicle(vehicle);
        }

        MatsimVehicleWriter writer = new MatsimVehicleWriter(vehicles);
        writer.writeFile("melun_test/input/vehicles.xml");
    }

    private void runModifyConfig() {
        Config config = ConfigUtils.loadConfig("melun_test/input/config.xml");
        config.controler().setOutputDirectory("melun_test/output");
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
        config.vehicles().setVehiclesFile("vehicles.xml");
        ConfigUtils.writeConfig(config, "melun_test/input/config_emissions.xml");
    }

    private void runModifyNetwork() {
        Config config = ConfigUtils.loadConfig("melun_test/input/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        for (Link link: network.getLinks().values()) {
            // this forces the OSM Mapping code to use URB/Local/50 as it the only thing we have in the sample HBEFA.
            NetworkUtils.setType(link, "tertiary");
            link.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, 50 / 3.6);
        }
        NetworkUtils.writeNetwork(network, "melun_test/input/network.xml.gz");
    }

    private void runMelunEmissions() throws CommandLine.ConfigurationException {
        RunComputeEmissionsEvents.main(new String[] {
                "--config-path", "melun_test/input/config_emissions.xml",
                "--hbefa-cold-avg", "sample_41_EFA_ColdStart_vehcat_2020average.csv",
                "--hbefa-hot-avg", "sample_41_EFA_HOT_vehcat_2020average.csv",
                "--hbefa-cold-detailed", "sample_41_EFA_ColdStart_SubSegm_2020detailed.csv",
                "--hbefa-hot-detailed", "sample_41_EFA_HOT_SubSegm_2020detailed.csv",
        });

        RunExportEmissionsNetwork.main(new String[] {
                "--config-path", "melun_test/input/config_emissions.xml",
                "--pollutants", "PM,CO,NOx,Unknown",
                "--time-bin-size", "3600"
        });

        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures("melun_test/output/emissions_network.shp");
        assert features.size() == 32999;
        SimpleFeature feature = features.stream().filter(f ->
                f.getAttribute("link").toString().equals("163994")
                & f.getAttribute("time").toString().equals("43200")
        ).findFirst().orElse(null);
        assert feature != null;
        assert feature.getAttribute("PM").equals(0.006847378350421);
        assert feature.getAttribute("CO").equals(0.456258730331835);
        assert feature.getAttribute("NOx").equals(0.477558671071797);
        assert feature.getAttribute("Unknown").equals(Double.NaN);

        // TODO : test RunComputeEmissionsGrid
    }

    @Test
    public void runTestEmissions() throws CommandLine.ConfigurationException {
        runCreateVehicles();
        runModifyConfig();
        runModifyNetwork();
        runMelunSimulation();
        runMelunEmissions();
    }

}
