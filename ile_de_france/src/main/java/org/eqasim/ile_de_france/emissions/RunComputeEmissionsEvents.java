package org.eqasim.ile_de_france.emissions;

import org.apache.commons.lang3.ArrayUtils;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;

// TODO: will need to be updated after the matsim 14 release to profit from https://github.com/matsim-org/matsim-libs/pull/1859

public class RunComputeEmissionsEvents {

    public static void main(String[] args) throws CommandLine.ConfigurationException {

        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path", "hbefa-cold-avg", "hbefa-hot-avg") //
                .allowOptions("hbefa-cold-detailed", "hbefa-hot-detailed")
                .build();

        ConfigGroup[] configGroups = ArrayUtils.addAll(new IDFConfigurator().getConfigGroups(), new EmissionsConfigGroup());

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configGroups);
        cmd.applyConfiguration(config);

        EmissionsConfigGroup emissionsConfig = (EmissionsConfigGroup) config.getModule("emissions");
        emissionsConfig.setHbefaVehicleDescriptionSource(EmissionsConfigGroup.HbefaVehicleDescriptionSource.asEngineInformationAttributes);
        emissionsConfig.setHbefaRoadTypeSource(EmissionsConfigGroup.HbefaRoadTypeSource.fromLinkAttributes);
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

        OsmHbefaMapping osmHbefaMapping = OsmHbefaMapping.build();
        Network network = scenario.getNetwork();
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

        final String outputDirectory = scenario.getConfig().controler().getOutputDirectory() + "/";
        EventWriterXML emissionEventWriter = new EventWriterXML( outputDirectory + "output_emissions_events.xml.gz" ) ;
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile( outputDirectory + "./output_events.xml.gz" );
        eventsManager.finishProcessing();

        emissionEventWriter.closeFile();
    }
}
