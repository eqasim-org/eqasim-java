package org.eqasim.ile_de_france.trb2020;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.replanning.TripListConverter;
import com.google.inject.Provider;
import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.ile_de_france.mode_choice.costs.CarCostModel;
import org.eqasim.ile_de_france.mode_choice.costs.PtCostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.CostParameters;
import org.eqasim.ile_de_france.mode_choice.parameters.ModeChoiceParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.*;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.*;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class UtilityShareCalculator {
	private final int numberOfThreads;
	private final int batchSize;
	private final Provider<TripRouter> routerProvider;
	private final ActivityFacilities facilities;

	public UtilityShareCalculator(int numberOfThreads, int batchSize, Provider<TripRouter> routerProvider, ActivityFacilities facilities) {
		this.numberOfThreads = numberOfThreads;
		this.batchSize = batchSize;
		this.routerProvider = routerProvider;
		this.facilities = facilities;
	}

	public void run(Population population, BufferedWriter writer) throws InterruptedException, IOException {
		List<Thread> threads = new LinkedList<>();

		writer.write(new CSVUtilityShareFormatter(";").formatHeader() + "\n");
		writer.flush();

		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();
		ParallelProgress progress = new ParallelProgress("Routing population ...", population.getPersons().size());

		for (int i = 0; i < numberOfThreads; i++) {
			Thread thread = new Thread(new Worker(personIterator, progress, writer));
			threads.add(thread);
		}

		threads.forEach(Thread::start);
		progress.start();

		for (Thread thread : threads) {
			thread.join();
		}

		writer.flush();
		writer.close();
		progress.close();
	}

	private class Worker implements Runnable {
		private final Iterator<? extends Person> personIterator;
		private final ParallelProgress progress;
		private final BufferedWriter writer;

		Worker(Iterator<? extends Person> personIterator, ParallelProgress progress, BufferedWriter writer) {
			this.personIterator = personIterator;
			this.progress = progress;
			this.writer = writer;
		}

		@Override
		public void run() {
			List<Person> localTasks = new LinkedList<>();
			TripRouter router = routerProvider.get();
			String[] mainModes = new String[]{"car", "pt", "bike", "walk"};

			TripsToLegsAlgorithm tripsToLegsAlgorithm = new TripsToLegsAlgorithm(router.getStageActivityTypes(), router.getMainModeIdentifier());

			CostParameters costParameters = CostParameters.buildDefault();
			ModeChoiceParameters modeChoiceParameters = ModeChoiceParameters.buildDefault();

			PersonPredictor personPredictor = new PersonPredictor();

			CarPredictor carPredictor = new CarPredictor(modeChoiceParameters, new CarCostModel(costParameters));
			PtPredictor ptPredictor = new PtPredictor(new PtCostModel());
			BikePredictor bikePredictor = new BikePredictor();
			WalkPredictor walkPredictor = new WalkPredictor();

			do {
				localTasks.clear();

				synchronized (personIterator) {
					while (personIterator.hasNext() && localTasks.size() < batchSize) {
						localTasks.add(personIterator.next());
					}
				}

				for (Person person : localTasks) {

					PersonVariables personVariables = personPredictor.predict(person);

					for (Plan plan : person.getPlans()) {

						// remove interaction activities
						tripsToLegsAlgorithm.run(plan);

						// get discrete mode choice trips
						List<? extends PlanElement> elements = plan.getPlanElements();
						List<DiscreteModeChoiceTrip> trips = new ArrayList<>((elements.size() - 1) / 2);
						List<Leg> legs = new ArrayList<>((elements.size() - 1) / 2);
						TripListConverter.convert(plan, trips, legs);

						for (DiscreteModeChoiceTrip trip : trips) {
							Facility originFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), facilities);
							Facility destinationFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), facilities);

							UtilityShareItem item = new UtilityShareItem(person.getId());

							for (String mode : mainModes) {

								List<? extends PlanElement> routedElements = router.calcRoute(mode, originFacility, destinationFacility,
										trip.getDepartureTime(), person);


								if (mode.equals(TransportMode.car)) {
									CarVariables carVariables = carPredictor.predict(trip, routedElements);
									item.setCarVariables(carVariables);

								} else if (mode.equals(TransportMode.pt)) {
									PtVariables ptVariables = ptPredictor.predict(personVariables, trip, routedElements);
									item.setPtVariables(ptVariables);

								} else if (mode.equals(TransportMode.bike)) {
									BikeVariables bikeVariables = bikePredictor.predict(routedElements);
									item.setBikeVariables(bikeVariables);

								} else if (mode.equals(TransportMode.walk)) {
									WalkVariables walkVariables = walkPredictor.predict(routedElements);
									item.setWalkVariables(walkVariables);
								}
							}

							// write variables to csv
							try {
								writer.write(new CSVUtilityShareFormatter(";").formatItem(item) + "\n");
								writer.flush();

							} catch (IOException exception) {
								exception.printStackTrace();
							}


						}



					}
				}

				progress.update(localTasks.size());
			} while (localTasks.size() > 0);
		}
	}
}
