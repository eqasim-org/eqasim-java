package org.eqasim.core.scenario.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import com.google.inject.Provider;

public class PopulationRouter {
	private final int numberOfThreads;
	private final int batchSize;
	private final boolean replaceExistingRoutes;
	private final Provider<PlanRouter> routerProvider;
	private final Set<String> modes;

	public PopulationRouter(int numberOfThreads, int batchSize, boolean replaceExistingRoutes, Set<String> modes,
			Provider<PlanRouter> routerProvider) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.routerProvider = routerProvider;
		this.replaceExistingRoutes = replaceExistingRoutes;
		this.modes = modes;
	}

	public void run(Population population) throws InterruptedException {
		List<Person> allPersons = new ArrayList<>(population.getPersons().values());
		int total = allPersons.size();
		ParallelProgress progress = new ParallelProgress("Routing population …", total);

		ExecutorService exec = Executors.newFixedThreadPool(numberOfThreads);
		AtomicBoolean errorOccurred = new AtomicBoolean(false);

		// One PlanRouter instance per thread
		ThreadLocal<PlanRouter> routerLocal = ThreadLocal.withInitial(() -> routerProvider.get());

		// Build independent chunk lists
		List<List<Person>> chunks = new ArrayList<>();
		for (int i = 0; i < total; i += batchSize) {
			int end = Math.min(i + batchSize, total);
			chunks.add(new ArrayList<>(allPersons.subList(i, end)));
		}

		// Submit one task per chunk
		for (List<Person> chunk : chunks) {
			exec.submit(() -> {
				try {
					PlanRouter router = routerLocal.get();
					for (Person p : chunk) {
						for (Plan plan : p.getPlans()) {
							router.run(plan, replaceExistingRoutes, modes);
						}
					}
					progress.update(chunk.size());

				} catch (Exception e) {
					e.printStackTrace();
					errorOccurred.set(true);
				}
			});
		}

		
		progress.start();
		exec.shutdown();
		exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		progress.close();

		if (errorOccurred.get()) {
			throw new RuntimeException("Found errors in routing threads");
		}
	}

}
