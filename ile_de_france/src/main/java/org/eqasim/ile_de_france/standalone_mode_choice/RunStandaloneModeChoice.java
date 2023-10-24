package org.eqasim.ile_de_france.standalone_mode_choice;


import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.eqasim.core.analysis.DefaultPersonAnalysisFilter;
import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.analysis.trips.TripItem;
import org.eqasim.core.analysis.trips.TripReaderFromPopulation;
import org.eqasim.core.analysis.trips.TripWriter;
import org.eqasim.core.components.transit_with_abstract_access.AbstractAccessModule;
import org.eqasim.core.components.transit_with_abstract_access.AbstractAccessModuleConfigGroup;
import org.eqasim.core.components.travel_time.RecordedTravelTime;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.validation.ScenarioValidator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class offers the functionality of running the discrete mode choice model on the whole population without having to go through the whole iterative MATSim process. It is also possible to filter-out the persons that do not have a valid alternative.
 * The class requires one parameter:
 * - config-path: a path to a MATSim config file
 * The mode choice is performed via a StandaloneModeChoice module which is configurable via a config group.
 * The StandaloneModeChoiceConfigGroup can be included in the supplied config file, if not one with the default settings is added and these settings can be set via the commandline using the config: prefix. Below the list of supported parameters:
 * - outputDirectory: The directory in which the resulting plans will as well as the logfiles be written
 * - removePersonsWithNoValidAlternatives: if set to true, persons with no valid alternative for at least one tour or trip will be removed in the resulting population
 * More parameters can be supplied via the command line
 * - write-input-csv-trips: if specified, writes out the base trips into a csv file called input_trips.csv before performing the mode choice
 * - write-output-csv-trips: writes out the trips resulting from the mode choice as a csv file called output_trips.csv in addition to the plans file
 * - travel-times-factors-path: if provided, should point out to a csv file specifying the congestion levels on the network during the day as factors by which the free speed is divided. The file in question is a csv With a header timeUpperBound;travelTimeFactor in which the timeUpperBound should be ordered incrementally.
 * - recorded-travel-times-path: mutually exclusive with the travel-times-factors-path. Points to a RecordedTravelTime file.
 * - simulate-after: if set, a single-iteration simulation using the resulting population will be performed, allowing to generate the regular MATSim output files.
 */
public class RunStandaloneModeChoice {
    public static class TravelTimeFactors implements TravelTime {

        private final String filePath;
        private final FreeSpeedTravelTime freeSpeedTravelTime;
        private List<Double> congestionSlotUpperBounds;
        private List<Double> congestionSlotSpeedFactor;
        private static final String CSV_SEPARATOR = ";";
        private static final String TIME_UPPER_BOUND_COLUMN = "timeUpperBound";
        private static final String CONGESTION_FACTOR_COLUMN = "travelTimeFactor";

        public TravelTimeFactors(String filePath) {
            this.filePath = filePath;
            this.freeSpeedTravelTime = new FreeSpeedTravelTime();
            try {
                this.readFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void readFile() throws IOException {
            this.congestionSlotSpeedFactor = new ArrayList<>();
            this.congestionSlotUpperBounds = new ArrayList<>();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(this.filePath)));
            String line;
            List<String> header = null;
            while ((line = reader.readLine()) != null) {
                List<String> row = Arrays.asList(line.split(CSV_SEPARATOR));

                if (header == null) {
                    header = row;
                } else {
                    double timeUpperBound = Double.parseDouble(row.get(header.indexOf(TIME_UPPER_BOUND_COLUMN)));
                    double speedFactor = Double.parseDouble(row.get(header.indexOf(CONGESTION_FACTOR_COLUMN)));
                    if(this.congestionSlotUpperBounds.size() > 0 && this.congestionSlotUpperBounds.get(this.congestionSlotUpperBounds.size()-1) >= timeUpperBound) {
                        throw new IllegalStateException();
                    }
                    this.congestionSlotUpperBounds.add(timeUpperBound);
                    this.congestionSlotSpeedFactor.add(speedFactor);
                }
            }
            reader.close();
        }

        @Override
        public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
            int slotIndex;
            for(slotIndex=this.congestionSlotSpeedFactor.size()-1; slotIndex>0 && congestionSlotUpperBounds.get(slotIndex)>time; slotIndex--);
            if(slotIndex < 0) {
                slotIndex=0;
            }
            return this.freeSpeedTravelTime.getLinkTravelTime(link, time, person, vehicle) * congestionSlotSpeedFactor.get(slotIndex);
        }
    }


    public static final String CMD_WRITE_INPUT_CSV = "write-input-csv-trips";
    public static final String CMD_WRITE_OUTPUT_CSV = "write-output-csv-trips";
    public static final String CMD_SIMULATE_AFTER = "simulate-after";
    public static final String CMD_CONFIG_PATH = "config-path";
    public static final String CMD_TRAVEL_TIMES_FACTORS_PATH = "travel-times-factors-path";
    public static final String CMD_RECORDED_TRAVEL_TIMES_PATH = "recorded-travel-times-path";


    public static void main(String[] args) throws CommandLine.ConfigurationException, InterruptedException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions(CMD_CONFIG_PATH)
                .allowOptions(CMD_WRITE_INPUT_CSV, CMD_WRITE_OUTPUT_CSV)
                .allowOptions(CMD_TRAVEL_TIMES_FACTORS_PATH, CMD_RECORDED_TRAVEL_TIMES_PATH)
                .allowOptions(CMD_SIMULATE_AFTER)
                .build();

        // Loading the config
        IDFConfigurator configurator = new IDFConfigurator();
        ConfigGroup[] configGroups = new ConfigGroup[configurator.getConfigGroups().length+1];
        int i=0;
        for(ConfigGroup configGroup: configurator.getConfigGroups()) {
            configGroups[i] = configGroup;
            i++;
        }
        // We should add this module now so that parameters can be overridden by the commandline
        configGroups[i] = new StandaloneModeChoiceConfigGroup();

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configGroups);
        configurator.addOptionalConfigGroups(config);
        cmd.applyConfiguration(config);

        Optional<String> travelTimesFactorsPath = cmd.getOption("travel-times-factors-path");
        Optional<String> recordedTravelTimesPath = cmd.getOption("recorded-travel-times-path");


        if(travelTimesFactorsPath.isPresent() && recordedTravelTimesPath.isPresent()) {
            throw new IllegalStateException("Can't use the two options 'travel-times-factors-path' and 'recorded-travel-times-path' simultaneously");
        }

        // We make sure the config is set to use DiscreteModeChoice, i.e. contains a DiscreteModeChoice module
        if(!config.getModules().containsKey("DiscreteModeChoice")) {
            throw new IllegalStateException("The config file is not set to use DiscreteModeChoice");
        }

        Scenario scenario = ScenarioUtils.createScenario(config);
        ScenarioUtils.loadScenario(scenario);

        ScenarioValidator scenarioValidator = new ScenarioValidator();
        scenarioValidator.checkScenario(scenario);


        InjectorBuilder injectorBuilder = new InjectorBuilder(scenario)
                // We add a module that just binds the PersonAnalysisFilter without having to add the whole EqasimAnalysisModule
                // This bind is required for building the TripReaderFromPopulation object
                .addOverridingModule(new AbstractModule() {
                    @Override
                    public void install() {
                        bind(PersonAnalysisFilter.class).to(DefaultPersonAnalysisFilter.class);
                    }
                })
                .addOverridingModule(new TimeInterpretationModule())
                .addOverridingModule(new EqasimModeChoiceModule())
                .addOverridingModule(new IDFModeChoiceModule(cmd))
                .addOverridingModule(new StandaloneModeChoiceModule(config));


        if(config.getModules().containsKey(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME)) {
            injectorBuilder.addOverridingModule(new AbstractAccessModule((AbstractAccessModuleConfigGroup) config.getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME)));
        }


        travelTimesFactorsPath.ifPresent(path -> {
            injectorBuilder.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addTravelTimeBinding("car").toInstance(new TravelTimeFactors(path));
                }
            });
        });

        recordedTravelTimesPath.ifPresent(path -> {
            injectorBuilder.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addTravelTimeBinding("car").to(RecordedTravelTime.class);
                }

                @Provides
                @Singleton
                RecordedTravelTime provideRecordedTravelTime() {
                    try {
                        InputStream inputStream = new FileInputStream(path);
                        RecordedTravelTime recordedTravelTime = RecordedTravelTime.readBinary(inputStream);
                        inputStream.close();
                        return recordedTravelTime;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });

        for(AbstractModule module: configurator.getModules()) {
            injectorBuilder.addOverridingModule(module);
        }

        com.google.inject.Injector injector = injectorBuilder.build();


        Population population = injector.getInstance(Population.class);
        // We initialize the TripReaderFromPopulation here as we might need it just below
        TripReaderFromPopulation tripReader = new TripReaderFromPopulation(Arrays.asList("car,pt".split(",")), injector.getInstance(MainModeIdentifier.class), injector.getInstance(PersonAnalysisFilter.class), Optional.empty(), Optional.empty());
        OutputDirectoryHierarchy outputDirectoryHierarchy = injector.getInstance(Key.get(OutputDirectoryHierarchy.class, Names.named("StandaloneModeChoice")));

        cmd.getOption(CMD_WRITE_INPUT_CSV).ifPresent(s -> {
            if(Boolean.parseBoolean(s)) writeTripsCsv(population, outputDirectoryHierarchy.getOutputFilename("input_trips.csv"), tripReader);
        });

        StandaloneModeChoicePerformer modeChoicePerformer = injector.getInstance(StandaloneModeChoicePerformer.class);

        modeChoicePerformer.run();

        cmd.getOption(CMD_WRITE_OUTPUT_CSV).ifPresent(s -> {
            if(Boolean.parseBoolean(s)) writeTripsCsv(population, outputDirectoryHierarchy.getOutputFilename("output_trips.csv"), tripReader);
        });


        if(cmd.getOption(CMD_SIMULATE_AFTER).isPresent()) {
            scenario.getConfig().controler().setFirstIteration(0);
            scenario.getConfig().controler().setLastIteration(0);
            Controler controller = new Controler(scenario);
            configurator.configureController(controller);
            controller.addOverridingModule(new EqasimAnalysisModule());
            controller.addOverridingModule(new EqasimModeChoiceModule());
            controller.addOverridingModule(new IDFModeChoiceModule(cmd));
            controller.run();
        }
    }

    private static void writeTripsCsv(Population population, String filePath, TripReaderFromPopulation tripReader) {
        //We write the initial trip modes
        Collection<TripItem> trips = tripReader.readTrips(population);
        try {
            new TripWriter(trips, DistanceUnit.meter, DistanceUnit.meter).write(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
