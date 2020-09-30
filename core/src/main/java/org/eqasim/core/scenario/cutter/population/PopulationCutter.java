package org.eqasim.core.scenario.cutter.population;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eqasim.core.misc.Constants;
import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;

import com.google.inject.Provider;

public class PopulationCutter {
	private final Provider<PlanCutter> planCutterProvider;
	private final PopulationFactory populationFactory;
	private final int numberOfThreads;
	private final int batchSize;

	public PopulationCutter(Provider<PlanCutter> planCutterProvider, PopulationFactory populationFactory,
			int numberOfThreads, int batchSize) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.planCutterProvider = planCutterProvider;
		this.populationFactory = populationFactory;
	}

	public void run(Population population) throws InterruptedException {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

		List<Thread> threads = new LinkedList<>();

		ParallelProgress progress = new ParallelProgress("Cutting population ...", population.getPersons().size());
		progress.start();

		AtomicBoolean errorsOccured = new AtomicBoolean(false);

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(personIterator, progress, planCutterProvider));
			thread.setUncaughtExceptionHandler((t, e) -> {
				e.printStackTrace();
				errorsOccured.set(true);
			});

			thread.start();
			threads.add(thread);
		}

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();

		if (errorsOccured.get()) {
			throw new RuntimeException("Errors occured while cutting the population.");
		}
	}

	private class Worker implements Runnable {
		private final Iterator<? extends Person> personIterator;
		private final ParallelProgress progress;
		private final Provider<PlanCutter> planCutterProvider;

		Worker(Iterator<? extends Person> personIterator, ParallelProgress progress,
				Provider<PlanCutter> planCutterProvider) {
			this.progress = progress;
			this.personIterator = personIterator;
			this.planCutterProvider = planCutterProvider;
		}

		@Override
		public void run() {
			List<Person> localTasks = new LinkedList<>();
			PlanCutter planCutter = planCutterProvider.get();

			do {
				localTasks.clear();

				synchronized (personIterator) {
					while (personIterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(personIterator.next());
					}
				}

				for (Person person : localTasks) {
					boolean isPersonOutside = false;

					List<Plan> newPlans = new LinkedList<>();
					List<Plan> oldPlans = new LinkedList<>();

					for (Plan oldPlan : person.getPlans()) {
						List<PlanElement> newPlanElements = planCutter.processPlan(oldPlan.getPlanElements());

						Plan newPlan = populationFactory.createPlan();

						for (int k = 0; k < newPlanElements.size(); k++) {
							if (k % 2 == 0) {
								Activity activity = (Activity) newPlanElements.get(k);
								newPlan.addActivity(activity);

								if (activity.getType().equals(Constants.OUTSIDE_ACTIVITY_TYPE)) {
									isPersonOutside = true;
								}
							} else {
								newPlan.addLeg((Leg) newPlanElements.get(k));
							}
						}

						newPlans.add(newPlan);
						oldPlans.add(oldPlan);
					}

					oldPlans.forEach(person::removePlan);
					newPlans.forEach(person::addPlan);

					person.getAttributes().putAttribute(Constants.OUTSIDE_AGENT_ATTRIBUTE, isPersonOutside);
				}

				progress.update(localTasks.size());
			} while (localTasks.size() > 0);
		}
	}
}
