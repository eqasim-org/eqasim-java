package org.eqasim.core.analysis.od_routing;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.analysis.od_routing.data.Location;
import org.eqasim.core.analysis.od_routing.data.LocationFacility;
import org.eqasim.core.analysis.od_routing.data.ModalOriginDestinationPair;
import org.eqasim.core.analysis.od_routing.data.ModalTravelTimeMatrix;
import org.eqasim.core.analysis.od_routing.data.OriginDestinationIterator;
import org.eqasim.core.analysis.od_routing.data.OriginDestinationPair;
import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;
import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;

import com.google.inject.Provider;

public class OriginDestinationRouter {
	private final int numberOfThreads;
	private final int batchSize;
	private final Provider<TripRouter> routerProvider;

	public OriginDestinationRouter(int numberOfThreads, int batchSize, Provider<TripRouter> routerProvider) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.routerProvider = routerProvider;
	}

	public ModalTravelTimeMatrix run(Collection<Location> locations, Set<String> modes, double departureTime)
			throws InterruptedException {
		List<Thread> threads = new LinkedList<>();

		OriginDestinationIterator iterator = new OriginDestinationIterator(locations);
		ModalTravelTimeMatrix matrix = new ModalTravelTimeMatrix(locations, modes);
		ParallelProgress progress = new ParallelProgress("Accessibility routing ...",
				locations.size() * locations.size());

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(iterator, matrix, departureTime, modes, progress));
			thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					throw new RuntimeException(e);
				}
			});
			threads.add(thread);
		}

		threads.forEach(Thread::start);
		progress.start();

		for (Thread thread : threads) {
			thread.join();
		}

		progress.close();
		return matrix;
	}

	private class Worker implements Runnable {
		private final OriginDestinationIterator iterator;
		private final ParallelProgress progress;
		private final ModalTravelTimeMatrix matrix;
		private final double departureTime;
		private final Set<String> modes;

		Worker(OriginDestinationIterator iterator, ModalTravelTimeMatrix matrix, double departureTime,
				Set<String> modes, ParallelProgress progress) {
			this.iterator = iterator;
			this.progress = progress;
			this.matrix = matrix;
			this.departureTime = departureTime;
			this.modes = modes;
		}

		@Override
		public void run() {
			List<OriginDestinationPair> localTasks = new LinkedList<>();
			Map<ModalOriginDestinationPair, Double> localResults = new HashMap<>();

			TripRouter router = routerProvider.get();

			do {
				localTasks.clear();
				localResults.clear();

				synchronized (iterator) {
					while (iterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(iterator.next());
					}
				}

				for (OriginDestinationPair pair : localTasks) {
					for (String mode : modes) {
						Facility fromFacility = new LocationFacility(pair.getOrigin());
						Facility toFacility = new LocationFacility(pair.getDestination());

						List<? extends PlanElement> elements = router.calcRoute(mode, fromFacility, toFacility,
								departureTime, null);

						double travelTime = 0.0;
						boolean isFirst = true;

						//System.out.println(pair.getOrigin().getId() + " ---> " + pair.getDestination().getId());

						for (Leg leg : TripStructureUtils.getLegs(elements)) {
							travelTime += leg.getTravelTime();

							if (leg.getMode().equals(TransportMode.pt)) {
								EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();

								if (isFirst) {
									//travelTime -= route.getWaitingTime();
								}

								isFirst = false;
							}

							if (leg.getMode().contains("walk")) {
								if (leg.getTravelTime() > 30.0 * 60.0) {
									//travelTime = Double.POSITIVE_INFINITY;
								}
							}

							//System.out.println(" " + leg.getMode() + " " + Time.writeTime(travelTime) + " " + leg.getRoute().toString());
							//System.out.println(" " + leg.getRoute().getRouteDescription());
						}

						localResults.put(new ModalOriginDestinationPair(mode, pair), travelTime);
					}
				}

				synchronized (matrix) {
					for (Map.Entry<ModalOriginDestinationPair, Double> entry : localResults.entrySet()) {
						matrix.setValue(entry.getKey().getMode(), entry.getKey().getOrigin(),
								entry.getKey().getDestination(), entry.getValue());
					}
				}

				progress.update(localTasks.size());
			} while (localTasks.size() > 0);
		}
	}
}
