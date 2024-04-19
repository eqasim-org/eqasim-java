package org.eqasim.ile_de_france.standalone_mode_choice;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.scenario.routing.RunPopulationRouting;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.replanning.DiscreteModeChoiceAlgorithm;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.misc.Counter;

import com.google.inject.Provider;

public class StandaloneModeChoicePerformer {

    private static final Logger logger = LogManager.getLogger(StandaloneModeChoicePerformer.class);

    private final Provider<DiscreteModeChoiceModel> discreteModeChoiceModelProvider;
    private final boolean removePersonsWithBadPlans;
    private final Population population;
    private final int numberOfThreads;
    private final long seed;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final Scenario scenario;

    public StandaloneModeChoicePerformer(Provider<DiscreteModeChoiceModel> discreteModeChoiceModelProvider, StandaloneModeChoiceConfigGroup configGroup, Population population, int numberOfThreads, long seed, OutputDirectoryHierarchy outputDirectoryHierarchy, Scenario scenario) {
        this.discreteModeChoiceModelProvider = discreteModeChoiceModelProvider;
        this.removePersonsWithBadPlans = configGroup.isRemovePersonsWithNoValidAlternative();
        this.numberOfThreads = numberOfThreads;
        this.population = population;
        this.seed = seed;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.scenario = scenario;
    }

    public void run() throws InterruptedException {

        Counter counter = new Counter("handled plan #");

        if(numberOfThreads > 0) {
            List<Thread> threads = new LinkedList<>();

            final AtomicBoolean errorOccurred = new AtomicBoolean(false);


            PlanAlgoThread[] planAlgoThreads = new PlanAlgoThread[this.numberOfThreads];

            for (int i = 0; i < numberOfThreads; i++) {
                Random random = new Random(this.seed);
                planAlgoThreads[i] = new PlanAlgoThread(new DiscreteModeChoiceAlgorithm(random, this.discreteModeChoiceModelProvider.get(), this.population.getFactory(), new TripListConverter()), counter, this.removePersonsWithBadPlans);
                Thread thread = new Thread(planAlgoThreads[i]);
                thread.setUncaughtExceptionHandler((t, e) -> {
                    e.printStackTrace();
                    errorOccurred.set(true);
                });
                threads.add(thread);
            }

            int personsCount = 0;
            logger.info(String.format("Distributing %d persons on %d threads", population.getPersons().size(), this.numberOfThreads));
            for(Person person: population.getPersons().values()) {
                List<Plan> unselectedPlans = new ArrayList<>();
                for(Plan plan: person.getPlans()) {
                    if(plan != person.getSelectedPlan()) {
                        unselectedPlans.add(plan);
                    }
                }
                unselectedPlans.forEach(person::removePlan);
                planAlgoThreads[personsCount % this.numberOfThreads].addPlanToThread(person.getSelectedPlan());
                personsCount+=1;
            }
            logger.info(String.format("Starting %d threads, handling in %d plans", this.numberOfThreads, population.getPersons().size()));

            threads.forEach(Thread::start);

            for (Thread thread: threads) {
                thread.join();
            }

            if (errorOccurred.get()) {
                throw new RuntimeException("Found errors in mode choice threads threads");
            }

            if(this.removePersonsWithBadPlans) {
                IdSet<Person> personsToRemove = new IdSet<>(Person.class);
                for(PlanAlgoThread planAlgoThread: planAlgoThreads) {
                    personsToRemove.addAll(planAlgoThread.getPersonsWithNoAlternative());
                }
                double percentage = ((double) personsToRemove.size()) * 100 / population.getPersons().size();
                logger.info(String.format("Removing %d persons with no valid alternative out of %d (%f %%)", personsToRemove.size(), population.getPersons().size(), percentage));
                for(Id<Person> personId: personsToRemove) {
                    population.removePerson(personId);
                }
            }
        } else {
            Random random = new Random(this.seed);
            PlanAlgoThread planAlgoThread = new PlanAlgoThread(new DiscreteModeChoiceAlgorithm(random, this.discreteModeChoiceModelProvider.get(), this.population.getFactory(), new TripListConverter()), counter, this.removePersonsWithBadPlans);
            for(Person person: population.getPersons().values()) {
                List<Plan> unselectedPlans = new ArrayList<>();
                for(Plan plan: person.getPlans()) {
                    if(plan != person.getSelectedPlan()) {
                        unselectedPlans.add(plan);
                    }
                }
                unselectedPlans.forEach(person::removePlan);
                planAlgoThread.addPlanToThread(person.getSelectedPlan());
            }
            planAlgoThread.run();
            if(this.removePersonsWithBadPlans) {
                IdSet<Person> personsToRemove = new IdSet<>(Person.class);
                personsToRemove.addAll(planAlgoThread.getPersonsWithNoAlternative());
                double percentage = ((double) personsToRemove.size()) * 100 / population.getPersons().size();
                logger.info(String.format("Removing %d persons with no valid alternative out of %d (%f %%)", personsToRemove.size(), population.getPersons().size(), percentage));
                personsToRemove.forEach(population::removePerson);
            }
        }

        String outputPlansName = outputDirectoryHierarchy.getOutputFilename("output_plans.xml.gz");
        // We do this right here before writing the population file so that following simulations using it work well
        RunPopulationRouting.clearVehicles(this.scenario.getConfig(), this.scenario);
        new PopulationWriter(population).write(outputPlansName);
        ConfigUtils.writeConfig(scenario.getConfig(), this.outputDirectoryHierarchy.getOutputFilename("output_config.xml"));
    }


    private final static class PlanAlgoThread implements Runnable {

        private final DiscreteModeChoiceAlgorithm planAlgo;
        private final List<Plan> plans = new LinkedList<>();
        private final Counter counter;
        private final IdSet<Person> personsWithNoAlternative;
        private final boolean reportPersonsWithNoAlternative;

        public PlanAlgoThread(final DiscreteModeChoiceAlgorithm algo, final Counter counter, boolean reportPersonsWithNoAlternative) {
            this.planAlgo = algo;
            this.counter = counter;
            this.personsWithNoAlternative = new IdSet<>(Person.class);
            this.reportPersonsWithNoAlternative = reportPersonsWithNoAlternative;
        }

        public void addPlanToThread(final Plan plan) {
            this.plans.add(plan);
        }

        @Override
        public void run() {
            for (Plan plan : this.plans) {
                try {
                    this.planAlgo.run(plan);
                } catch (IllegalStateException e) {
                    if(e.getCause() instanceof DiscreteModeChoiceModel.NoFeasibleChoiceException) {
                        if(this.reportPersonsWithNoAlternative) {
                            this.personsWithNoAlternative.add(plan.getPerson().getId());
                        }
                    } else {
                        throw e;
                    }
                }
                this.counter.incCounter();
            }
        }

        public IdSet<Person> getPersonsWithNoAlternative() {
            return this.personsWithNoAlternative;
        }
    }
}
