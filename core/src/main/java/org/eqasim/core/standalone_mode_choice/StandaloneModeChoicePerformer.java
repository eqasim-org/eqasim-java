package org.eqasim.core.standalone_mode_choice;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.misc.ParallelProgress;
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
    private final int chunkSize;
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
        this.chunkSize = configGroup.getChunkSize();
    }

    public void run() throws InterruptedException, ExecutionException {

        
        // we only collect the selected plans to perform mode-choice
        List<Plan> selectedPlans = new ArrayList<>(population.getPersons().size());
        for (Person person : population.getPersons().values()) {
            // remove unselected plans
            List<Plan> toRemove = new ArrayList<>();
            for (Plan plan : person.getPlans()) {
                if (plan != person.getSelectedPlan()) {
                    toRemove.add(plan);
                }
            }
            toRemove.forEach(person::removePlan);
            selectedPlans.add(person.getSelectedPlan());
        }
        
        ParallelProgress progress = new ParallelProgress("Standalone mode choice", selectedPlans.size());
        progress.start();

        if(numberOfThreads > 0 && !selectedPlans.isEmpty()) {
        	ExecutorService exec = Executors.newFixedThreadPool(numberOfThreads);
            List<Future<Set<Id<Person>>>> futures = new ArrayList<>();

            final int total = selectedPlans.size();
            int numChunks = (total + chunkSize - 1) / chunkSize;

            logger.info(String.format(
                    "Splitting %d plans into %d chunks (%,d each) over %d threads",
                    total, numChunks, chunkSize, numberOfThreads));
            
            Random random = new Random(this.seed);
            for (int chunk = 0; chunk < numChunks; chunk++) {
                final int from = chunk * chunkSize;
                final int to   = Math.min(from + chunkSize, total);

                List<Plan> subList = selectedPlans.subList(from, to);
                Random chunkRandom = new Random(random.nextInt());

                futures.add(exec.submit(() -> {
                    PlanAlgoThread worker = new PlanAlgoThread(
                        new DiscreteModeChoiceAlgorithm(
                            chunkRandom,
                            discreteModeChoiceModelProvider.get(),
                            population.getFactory(),
                            new TripListConverter()
                        ),
                        progress,
                        removePersonsWithBadPlans
                    );
                    // feed our little chunk
                    for (Plan plan : subList) {
                        worker.addPlanToThread(plan);
                    }
                    // run and collect any bad-plan IDs
                    worker.run();
                    return worker.getPersonsWithNoAlternative();
                }));
            }
            // wait for everything to finish
            exec.shutdown();
            exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // combine results
            IdSet<Person> personsToRemove = new IdSet<>(Person.class);
            for (Future<Set<Id<Person>>> f : futures) {
                personsToRemove.addAll(f.get());
            }

            if(this.removePersonsWithBadPlans && !personsToRemove.isEmpty()) {

                double percentage = ((double) personsToRemove.size()) * 100 / population.getPersons().size();
                logger.info(String.format("Removing %d persons with no valid alternative out of %d (%f %%)", personsToRemove.size(), population.getPersons().size(), percentage));
                for(Id<Person> personId: personsToRemove) {
                    population.removePerson(personId);
                }
            }
        } else {
            Random random = new Random(this.seed);
            PlanAlgoThread planAlgoThread = new PlanAlgoThread(new DiscreteModeChoiceAlgorithm(random, this.discreteModeChoiceModelProvider.get(), this.population.getFactory(), new TripListConverter()), progress, this.removePersonsWithBadPlans);
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

        progress.close();

        String outputPlansName = outputDirectoryHierarchy.getOutputFilename("output_plans.xml.gz");
        new PopulationWriter(population).write(outputPlansName);
        ConfigUtils.writeConfig(scenario.getConfig(), this.outputDirectoryHierarchy.getOutputFilename("output_config.xml"));
    }


    private final static class PlanAlgoThread implements Runnable {

        private final DiscreteModeChoiceAlgorithm planAlgo;
        private final List<Plan> plans = new LinkedList<>();
        private final ParallelProgress progress;
        private final IdSet<Person> personsWithNoAlternative;
        private final boolean reportPersonsWithNoAlternative;

        public PlanAlgoThread(final DiscreteModeChoiceAlgorithm algo, final ParallelProgress progress, boolean reportPersonsWithNoAlternative) {
            this.planAlgo = algo;
            this.progress = progress;
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
                this.progress.update(1);
            }
        }

        public IdSet<Person> getPersonsWithNoAlternative() {
            return this.personsWithNoAlternative;
        }
    }
}
