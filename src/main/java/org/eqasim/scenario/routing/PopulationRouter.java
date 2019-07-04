package org.eqasim.scenario.routing;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.misc.ParallelProgress;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.PlanRouter;

import com.google.inject.Provider;

public class PopulationRouter {
	private final int numberOfThreads;
	private final int batchSize;
	private final Provider<PlanRouter> routerProvider;

	public PopulationRouter(int numberOfThreads, int batchSize, Provider<PlanRouter> routerProvider) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.routerProvider = routerProvider;
	}

	public void run(Population population) throws InterruptedException {
		ThreadGroup threadGroup = new ThreadGroup("PopulationRouter");
		List<Thread> threads = new LinkedList<>();

		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();
		ParallelProgress progress = new ParallelProgress("Routing population ...", population.getPersons().size());

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(threadGroup, new Worker(personIterator, progress));
			thread.setDaemon(true);
			threads.add(thread);
		}

		threads.forEach(Thread::start);
		progress.start();

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();
	}

	private class Worker implements Runnable {
		private final Iterator<? extends Person> personIterator;
		private final ParallelProgress progress;

		Worker(Iterator<? extends Person> personIterator, ParallelProgress progress) {
			this.personIterator = personIterator;
			this.progress = progress;
		}

		@Override
		public void run() {
			List<Person> localTasks = new LinkedList<>();
			PlanRouter router = routerProvider.get();

			do {
				localTasks.clear();

				synchronized (personIterator) {
					while (personIterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(personIterator.next());
					}

					for (Person person : localTasks) {
						router.run(person);
					}
				}

				progress.update(localTasks.size());
			} while (localTasks.size() > 0);
		}
	}
}
