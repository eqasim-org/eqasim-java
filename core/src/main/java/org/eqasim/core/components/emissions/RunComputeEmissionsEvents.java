package org.eqasim.core.components.emissions;

import org.apache.commons.lang3.StringUtils;
import org.eqasim.core.misc.ClassUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RunComputeEmissionsEvents {

    public static void main(String[] args) throws CommandLine.ConfigurationException {

        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "hbefa-cold-avg", "hbefa-hot-avg") //
                .allowOptions("hbefa-cold-detailed", "hbefa-hot-detailed", "configurator-class")
                .build();
        
        EqasimConfigurator configurator;
        if(cmd.hasOption("configurator-class")) {
            configurator = ClassUtils.getInstanceOfClassExtendingOtherClass(cmd.getOptionStrict("configurator-class"), EqasimConfigurator.class);
        } else {
            configurator = new EqasimConfigurator();
        }

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        configurator.registerConfigGroup(new EmissionsConfigGroup(), false);
        configurator.updateConfig(config);
        cmd.applyConfiguration(config);

        EmissionsConfigGroup emissionsConfig = (EmissionsConfigGroup) config.getModules().get("emissions");
        emissionsConfig.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.asEngineInformationAttributes);
        emissionsConfig.setDetailedVsAverageLookupBehavior(
                EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
        emissionsConfig.setNonScenarioVehicles(EmissionsConfigGroup.NonScenarioVehicles.abort);
        emissionsConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);

        emissionsConfig.setAverageColdEmissionFactorsFile(cmd.getOptionStrict("hbefa-cold-avg"));
        emissionsConfig.setAverageWarmEmissionFactorsFile(cmd.getOptionStrict("hbefa-hot-avg"));

        if (cmd.hasOption("hbefa-cold-detailed") && cmd.hasOption("hbefa-hot-detailed")) {
            emissionsConfig.setDetailedColdEmissionFactorsFile(cmd.getOptionStrict("hbefa-cold-detailed"));
            emissionsConfig.setDetailedWarmEmissionFactorsFile(cmd.getOptionStrict("hbefa-hot-detailed"));
        }

        Scenario scenario = ScenarioUtils.createScenario(config);
        ScenarioUtils.loadScenario(scenario);

        // the default hbefa type is URB/Acess/30 but can be changed like this
        // SafeOsmHbefaMapping.defaultType = "URB/Local/50";
        SafeOsmHbefaMapping osmHbefaMapping = new SafeOsmHbefaMapping();

        Network network = scenario.getNetwork();
        // if the network is from pt2matsim it might not have "type" but "osm:way:highway" attribute instead
        for (Link link: network.getLinks().values()) {
            String roadTypeAttribute = NetworkUtils.getType(link);
            String osmRoadTypeAttribute = (String) link.getAttributes().getAttribute("osm:way:highway");
            if (StringUtils.isBlank(roadTypeAttribute)) {
                if (!StringUtils.isBlank(osmRoadTypeAttribute)) {
                    NetworkUtils.setType(link, osmRoadTypeAttribute);
                }
                else { // not a road (railway for example)
                    NetworkUtils.setType(link, "unclassified");
                }
            }
            // '_link' types are not defined in the OSM mapping, set t undefined
            if (NetworkUtils.getType(link).contains("_link")) {
                NetworkUtils.setType(link, "unclassified");
            }
            if (NetworkUtils.getType(link).equals("living_street")) {
                NetworkUtils.setType(link, "living");
            }
        }
        osmHbefaMapping.addHbefaMappings(network);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        AbstractModule module = new AbstractModule(){
            @Override
            public void install(){
                bind( Scenario.class ).toInstance( scenario );
                bind( EventsManager.class ).toInstance( eventsManager );
                bind( EmissionModule.class ) ;
            }
        };

        com.google.inject.Injector injector = Injector.createInjector(config, module );
        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        final String outputDirectory = scenario.getConfig().controller().getOutputDirectory() + "/";
        EventWriterXML emissionEventWriter = new EventWriterXML( outputDirectory + "output_emissions_events.xml.gz" ) ;
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile( outputDirectory + "./output_events.xml.gz" );
        eventsManager.finishProcessing();

        emissionEventWriter.closeFile();
    }
}
