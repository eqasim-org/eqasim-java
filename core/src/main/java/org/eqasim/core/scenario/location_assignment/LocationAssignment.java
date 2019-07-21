package org.eqasim.core.scenario.location_assignment;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.location_assignment.matsim.solver.MATSimAssignmentSolver;
import org.eqasim.core.location_assignment.matsim.solver.MATSimAssignmentSolverBuilder;
import org.eqasim.core.location_assignment.matsim.solver.MATSimSolverResult;
import org.eqasim.core.location_assignment.matsim.utils.LocationAssignmentPlanAdapter;
import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.scenario.location_assignment.listener.DefaultLocationAssignmentListener;
import org.eqasim.core.scenario.location_assignment.listener.LocationAssignmentListener;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

public class LocationAssignment {
	private final MATSimAssignmentSolverBuilder builder;
	private final int numberOfThreads;
	private final int batchSize;

	public LocationAssignment(MATSimAssignmentSolverBuilder builder, int numberOfThreads, int batchSize) {
		this.builder = builder;
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
	}
	
	public void run(Population population) throws InterruptedException {
		run(population, new DefaultLocationAssignmentListener());
	}

	public void run(Population population, LocationAssignmentListener listener) throws InterruptedException {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

		List<Thread> threads = new LinkedList<>();

		ParallelProgress progress = new ParallelProgress("Location assignment ...", population.getPersons().size());

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(personIterator, progress, listener));
			thread.start();
			threads.add(thread);
		}

		progress.start();

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();
	}

	class Worker implements Runnable {
		private final Iterator<? extends Person> personIterator;
		private final ParallelProgress progress;
		private final LocationAssignmentListener listener;

		public Worker(Iterator<? extends Person> personIterator, ParallelProgress progress,
				LocationAssignmentListener listener) {
			this.personIterator = personIterator;
			this.progress = progress;
			this.listener = listener;
		}

		@Override
		public void run() {
			List<Person> localTasks = new LinkedList<>();

			MATSimAssignmentSolver solver = builder.build();
			LocationAssignmentPlanAdapter adapter = new LocationAssignmentPlanAdapter();

			do {
				localTasks.clear();

				synchronized (personIterator) {
					while (personIterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(personIterator.next());
					}
				}

				for (Person person : localTasks) {
					for (Plan plan : person.getPlans()) {
						Collection<MATSimSolverResult> result = solver.solvePlan(plan);
						result.forEach(adapter::accept);
						listener.process(result);
					}
				}

				progress.update(localTasks.size());
				listener.update();
			} while (localTasks.size() > 0);
		}
	}
}
