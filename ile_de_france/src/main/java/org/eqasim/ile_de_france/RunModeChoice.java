package org.eqasim.ile_de_france;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.analysis.trips.TripItem;
import org.eqasim.core.analysis.trips.TripReaderFromPopulation;
import org.eqasim.core.analysis.trips.TripWriter;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.scenario.validation.ScenarioValidator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.*;


/**
 * This class isolates the discrete mode choice model component of Eqasim and performs the mode choice on all the agents of a provided population
 * It is meant to be ran from the command line with the following arguments
 *      - config-path: mandatory, the path of the MATSim config file indicating the population on which the mode choice model should be performed. This configuration also specifies the parameters of the mode choice model and other parameters.
 *      - output-plans-path: optional, when used, writes a plans file containing the new agent plans resulting from the mode choice
 *      - output-csv-path: optional, when used, writes a csv file with the header personId;tripId;mode indicating the new modes of agent trips
 *      - base-csv-path: optional, when used, writes a csv file containing the base modes of agent trips before performing the mode choice
 * At least one of the arguments output-plans-path and output-csv-path should be used.
 * Parameters in the configuration file can be overridden in the command line by using an argument of the form config:arg=value.
 * E.g. --config-path=config.xml --output-plans-path=plans_out.xml.gz --output-csv-path=trip_modes_out.csv --base-csv-path=trip_modes_in.csv --config:global.numberOfThreads=10
 */
public class RunModeChoice {

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

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path")
                .allowOptions("output-plans-path", "output-csv-path", "base-csv-path")
                .allowOptions("travel-times-factors-path")
                .build();

        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
        Optional<String> outputPlansPath = cmd.getOption("output-plans-path");
        Optional<String> outputCsvPath = cmd.getOption("output-csv-path");

        if(outputPlansPath.isEmpty() && outputCsvPath.isEmpty()) {
            throw new IllegalStateException("At least one of output-plans-path and output-csv-path should be provided");
        }

        IDFConfigurator configurator = new IDFConfigurator();
        for(ConfigGroup configGroup : configurator.getConfigGroups()) {
            config.addModule(configGroup);
        }
        cmd.applyConfiguration(config);

        // We make sure the config is set to use DiscreteModeChoice, i.e. contains a DiscreteModeChoice module and a DiscreteModeChoice strategy settings
        boolean containsDiscreteModeChoiceStrategy = false;
        for(StrategyConfigGroup.StrategySettings strategySettings: config.strategy().getStrategySettings()) {
            if(strategySettings.getStrategyName().equals("DiscreteModeChoice")) {
                containsDiscreteModeChoiceStrategy = true;
                break;
            }
        }
        if(!containsDiscreteModeChoiceStrategy || !config.getModules().containsKey("DiscreteModeChoice")) {
            throw new IllegalStateException("The config file is not set to use DiscreteModeChoice");
        }

        Scenario scenario = ScenarioUtils.createScenario(config);
        ScenarioUtils.loadScenario(scenario);

        ScenarioValidator scenarioValidator = new ScenarioValidator();
        scenarioValidator.checkScenario(scenario);

        InjectorBuilder injectorBuilder = new InjectorBuilder(scenario)
                .addOverridingModule(new NewControlerModule())
                .addOverridingModule(new ControlerDefaultCoreListenersModule())
                .addOverridingModule(new ControlerDefaultsModule())
                .addOverridingModule(new IDFModeChoiceModule(cmd))
                .addOverridingModule(new EqasimModeChoiceModule())
                .addOverridingModule(new EqasimAnalysisModule())
                .addOverridingModule(new ModelModule())
                .addOverridingModule(new DiscreteModeChoiceModule());
        if(cmd.hasOption("travel-times-factors-path")) {
            String travelTimesFactorsPath = cmd.getOptionStrict("travel-times-factors-path");
            injectorBuilder.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addTravelTimeBinding("car").toInstance(new TravelTimeFactors(travelTimesFactorsPath));
                }
            });
        }

        for(AbstractModule module: configurator.getModules()) {
            injectorBuilder.addOverridingModule(module);
        }
        Injector injector = injectorBuilder.build();


        Population population = injector.getInstance(Population.class);
        // We init the TripReaderFromPopulation here as we might need it just below
        // We retrieve the DiscreteModeChoice Strategy here
        /*
         * Depending on the configuration, the strategy can be set to use multiple threads or not.
         * In the former case, the threads need to be created before running the strategy.
         */

        TripReaderFromPopulation tripReader = new TripReaderFromPopulation(Arrays.asList("car,pt".split(",")), injector.getInstance(MainModeIdentifier.class), injector.getInstance(PersonAnalysisFilter.class), Optional.empty(), Optional.empty());
        cmd.getOption("base-csv-path").ifPresent(s -> {
            //We write the initial trip modes
            Collection<TripItem> trips = tripReader.readTrips(population);
            try {
                new TripWriter(trips, DistanceUnit.meter, DistanceUnit.meter).write(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


        PlanStrategy strategy = injector.getInstance(Key.get(PlanStrategy.class, Names.named("DiscreteModeChoice")));

        strategy.init(injector.getInstance(ReplanningContext.class));
        for(Person person: population.getPersons().values()) {
            strategy.run(person);
        }
        /*
         * In the multithreaded case, the run method only adds the person to the queue of a given thread.
         * We need to call the finish method to actually perform the mode choice.
         */
        strategy.finish();
        outputPlansPath.ifPresent(s -> new PopulationWriter(population).write(s));
        outputCsvPath.ifPresent(s -> {
            Collection<TripItem> trips = tripReader.readTrips(population);
            try {
                new TripWriter(trips, DistanceUnit.meter, DistanceUnit.meter).write(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
