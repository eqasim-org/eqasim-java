package org.eqasim.core.components.headway;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.facilities.Facility;

import com.google.inject.Provider;

public class HeadwayImputer {
	private final int numberOfThreads;
	private final int batchSize;
	private final boolean replaceExistingHeadways;

	private final Network network;

	private final Provider<HeadwayCalculator> calculatorProvider;

	public HeadwayImputer(int numberOfThreads, int batchSize, boolean replaceExistingHeadways, Network network,
			Provider<HeadwayCalculator> calculatorProvider) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.network = network;
		this.calculatorProvider = calculatorProvider;
		this.replaceExistingHeadways = replaceExistingHeadways;
	}

	public void run(Population population) throws InterruptedException {
		List<Thread> threads = new LinkedList<>();

		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();
		ParallelProgress progress = new ParallelProgress("Imputing headway ...", population.getPersons().size());

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(personIterator, progress));
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
			HeadwayCalculator calculator = calculatorProvider.get();

			do {
				localTasks.clear();

				synchronized (personIterator) {
					while (personIterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(personIterator.next());
					}
				}

				for (Person person : localTasks) {
					for (Plan plan : person.getPlans()) {
						for (Trip trip : TripStructureUtils.getTrips(plan)) {
							Activity originActivity = trip.getOriginActivity();

							if (originActivity.getAttributes().getAttribute("headway_min") == null
									|| replaceExistingHeadways) {
								Link originLink = network.getLinks().get(trip.getOriginActivity().getLinkId());
								Link destinationLink = network.getLinks()
										.get(trip.getDestinationActivity().getLinkId());

								Facility originFacility = new LinkWrapperFacility(originLink);
								Facility destinationFacility = new LinkWrapperFacility(destinationLink);

								double headway_min = calculator.calculateHeadway_min(originFacility,
										destinationFacility, trip.getOriginActivity().getEndTime().seconds());

								trip.getOriginActivity().getAttributes().putAttribute("headway_min", headway_min);
							}
						}
					}
				}

				progress.update(localTasks.size());
			} while (localTasks.size() > 0);
		}
	}
}
