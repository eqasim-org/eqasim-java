package org.eqasim.core.scenario.cutter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunScenarioCutterV2 {

    public static final String[] SHAPEFILE_EXTENSIONS = new String[]{".shp", ".cpg", ".dbf", ".qmd", ".shx", ".prj"};

    static public void main(String[] args)
            throws ConfigurationException, IOException, InterruptedException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "output-path", "extent-path", "vdf-travel-times-path") //
                .allowOptions("threads", "prefix", "extent-attribute", "extent-value", "plans-path", "events-path") //
                .allowOptions("flag-area-link-modes") //
                .build();

        String outputPath = cmd.getOptionStrict("output-path");

        EqasimConfigurator eqasimConfigurator = new EqasimConfigurator();
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        eqasimConfigurator.updateConfig(config);
        cmd.applyConfiguration(config);

        if(!config.getModules().containsKey(VDFConfigGroup.GROUP_NAME) || !config.getModules().containsKey(VDFEngineConfigGroup.GROUP_NAME)) {
            throw new IllegalStateException(String.format("This scenario cutter only works with configs where both '%s' and '%s' modules are used", VDFConfigGroup.GROUP_NAME, VDFEngineConfigGroup.GROUP_NAME));
        }

        List<String> scenarioCutterArgs = new ArrayList<>();
        for(String requiredOption: RunScenarioCutter.REQUIRED_ARGS) {
            scenarioCutterArgs.add("--"+requiredOption);
            scenarioCutterArgs.add(cmd.getOptionStrict(requiredOption));
        }
        for(String optionalOption: RunScenarioCutter.OPTIONAL_ARGS) {
            if(cmd.hasOption(optionalOption)) {
                scenarioCutterArgs.add("--"+optionalOption);
                scenarioCutterArgs.add(cmd.getOptionStrict(optionalOption));
            }
        }
        scenarioCutterArgs.add("--skip-routing");
        scenarioCutterArgs.add("true");

        RunScenarioCutter.main(scenarioCutterArgs.toArray(String[]::new));

        String prefix = cmd.getOption("prefix").orElse("");

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        eqasimConfigurator.configureScenario(scenario);
        // We first load the population resulting from the legacy cutter and store the person ids
        new PopulationReader(scenario).readFile(Paths.get(outputPath, prefix+"population.xml.gz").toString());
        IdSet<Person> personIds = new IdSet<>(Person.class);
        scenario.getPopulation().getPersons().values().stream().map(Person::getId).forEach(personIds::add);

        // We now read the data from the original scenario
        // Before we need to check if we are reading a file other than what's in the config
        if (cmd.hasOption("plans-path")) {
            File plansFile = new File(cmd.getOptionStrict("plans-path"));

            if (!plansFile.exists()) {
                throw new IllegalStateException("Plans file does not exist: " + plansFile.getPath());
            } else {
                config.plans().setInputFile(plansFile.getAbsolutePath());
            }
        }

        scenario = ScenarioUtils.createScenario(config);
        eqasimConfigurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        eqasimConfigurator.adjustScenario(scenario);

        // We remove from the original population, the persons that do not appear in the one resulting from the legacy cutter
        IdSet<Person> personsToRemove = new IdSet<>(Person.class);
        scenario.getPopulation().getPersons().values().stream().map(Person::getId).filter(personId -> !personIds.contains(personId)).forEach(personsToRemove::add);
        personsToRemove.forEach(scenario.getPopulation()::removePerson);

        // Now we process the network
        File extentPath = new File(cmd.getOptionStrict("extent-path"));
        Optional<String> extentAttribute = cmd.getOption("extent-attribute");
        Optional<String> extentValue = cmd.getOption("extent-value");
        ScenarioExtent extent = new ShapeScenarioExtent.Builder(extentPath, extentAttribute, extentValue).build();

        Set<String> insideModes = new HashSet<>();
        if(Boolean.parseBoolean(cmd.getOption("flag-area-link-modes").orElse("false"))) {
            scenario.getNetwork().getLinks().values()
                    .stream().filter(link -> extent.isInside(link.getFromNode().getCoord()) && extent.isInside(link.getFromNode().getCoord()))
                    .forEach(link -> {
                        Set<String> linkModes = new HashSet<>(link.getAllowedModes());
                        for(String mode: link.getAllowedModes()) {
                            String insideMode = "inside_"+mode;
                            insideModes.add(insideMode);
                            linkModes.add(insideMode);
                        }
                        link.setAllowedModes(linkModes);
                    });

            for(String mode: insideModes) {
                findLargestFullyConnectedSubnetwork(scenario.getNetwork(), mode);
            }
        }

        // "Cut" config
        // (we need to reload it, because it has become locked at this point)
        config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        eqasimConfigurator.updateConfig(config);
        cmd.applyConfiguration(config);
        ConfigCutter configCutter = new ConfigCutter(prefix);
        configCutter.run(config);

        // Before writing the config, we make sure we configure VDF to update the travel times only in the study area
        String extentBasePath = Paths.get(outputPath, "extent").toAbsolutePath().toString();
        String copiedExtentPath = Paths.get(extentBasePath, extentPath.getName()).toString();
        FileUtils.forceMkdir(new File(extentBasePath));
        copyExtentFiles(extentPath.getAbsolutePath(), copiedExtentPath);
        VDFConfigGroup vdfConfigGroup = VDFConfigGroup.getOrCreate(config);
        vdfConfigGroup.setUpdateAreaShapefile("extent/" + extentPath.getName());
        // We also set the VDF config to use the vdf.bin file for initial travel times
        vdfConfigGroup.setInputFile("vdf.bin");

        new ScenarioWriter(config, scenario, prefix).run(new File(outputPath).getAbsoluteFile());

        FileUtils.copyFile(new File(cmd.getOptionStrict("vdf-travel-times-path")), new File(outputPath, "vdf.bin"));
    }

    public static void findLargestFullyConnectedSubnetwork(Network network, String mode) {
        Network subNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(network).filter(subNetwork, Set.of(mode));

        NetworkUtils.runNetworkCleaner(subNetwork);

        for(Link link: network.getLinks().values()) {
            if(link.getAllowedModes().contains(mode) && !subNetwork.getLinks().containsKey(link.getId())) {
                Set<String> modes = new HashSet<>(link.getAllowedModes());
                modes.remove(mode);
                link.setAllowedModes(modes);
            }
        }
    }

    private static void copyExtentFiles(String sourcePath, String destPath) throws IOException {
        if(sourcePath.endsWith(".shp")) {
            sourcePath = sourcePath.substring(0, sourcePath.length()-4);
            destPath = destPath.substring(0, destPath.length()-4);
            for(String extension: SHAPEFILE_EXTENSIONS) {
                FileUtils.copyFile(new File(sourcePath + extension), new File(destPath + extension));
            }
        } else {
            FileUtils.copyFile(new File(sourcePath), new File(destPath));
        }
    }
}
