package org.eqasim.ile_de_france.travel_time;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

public class TravelTimeComparisonListener implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		IterationEndsListener, IterationStartsListener {
	private final Population population;
	private final OutputDirectoryHierarchy outputHierarchy;

	private final DescriptiveStatistics statistics = new DescriptiveStatistics();

	private final Map<Integer, Double> meanHistory = new HashMap<>();
	private final Map<Integer, Double> medianHistory = new HashMap<>();
	private final Map<Integer, Double> maxHistory = new HashMap<>();
	private final Map<Integer, Double> minHistory = new HashMap<>();
	private final Map<Integer, Double> q90History = new HashMap<>();
	private final Map<Integer, Double> q99History = new HashMap<>();
	private final Map<Integer, Double> q10History = new HashMap<>();

	private final Map<Id<Person>, PersonDepartureEvent> departureEvents = new HashMap<>();
	private final Map<Id<Person>, Integer> elementIndices = new HashMap<>();

	private boolean isConverged = false;

	private double minimumTravelTime;

	private double minimumDepartureTime;
	private double maximumDepartureTime;

	private double convergenceThreshold;
	private int detailedAnalysisInterval;

	private BufferedWriter detailedWriter = null;

	public TravelTimeComparisonListener(OutputDirectoryHierarchy outputHierarchy, Population population,
			double minimumTravelTime, double minimumDepartureTime, double maximumDepartureTime,
			double convergenceThreshold, int detailedAnalysisInterval) {
		this.population = population;
		this.outputHierarchy = outputHierarchy;
		this.minimumTravelTime = minimumTravelTime;
		this.minimumDepartureTime = minimumDepartureTime;
		this.maximumDepartureTime = maximumDepartureTime;
		this.convergenceThreshold = convergenceThreshold;
		this.detailedAnalysisInterval = detailedAnalysisInterval;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			departureEvents.put(event.getPersonId(), event);
		}
	}

	private Leg getNextCarLeg(Id<Person> personId) {
		int index = elementIndices.getOrDefault(personId, 0);

		List<? extends PlanElement> elements = population.getPersons().get(personId).getSelectedPlan()
				.getPlanElements();

		while (index < elements.size()) {
			PlanElement element = elements.get(index);

			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals("car")) {
					elementIndices.put(personId, index + 1);
					return (Leg) element;
				}
			}

			index++;
		}

		throw new IllegalStateException();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {
			PersonDepartureEvent departureEvent = departureEvents.remove(event.getPersonId());

			if (departureEvent != null) {
				double departureTime = departureEvent.getTime();

				if (departureTime >= minimumDepartureTime && departureTime < maximumDepartureTime) {
					double simulatedTravelTime = event.getTime() - departureTime;

					Leg leg = getNextCarLeg(event.getPersonId());
					double plannedTravelTime = ((NetworkRoute) leg.getRoute()).getTravelTime().seconds();

					if (plannedTravelTime >= minimumTravelTime) {
						statistics.addValue((simulatedTravelTime - plannedTravelTime) / plannedTravelTime);
					}

					if (detailedWriter != null) {
						try {
							detailedWriter.write(String.join(";", Arrays.asList( //
									event.getPersonId().toString(), //
									String.valueOf(elementIndices.get(event.getPersonId())), //
									String.valueOf(plannedTravelTime), //
									String.valueOf(simulatedTravelTime))) + "\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		meanHistory.put(event.getIteration(), statistics.getMean());
		medianHistory.put(event.getIteration(), statistics.getPercentile(50));
		maxHistory.put(event.getIteration(), statistics.getMax());
		q90History.put(event.getIteration(), statistics.getPercentile(90));
		q99History.put(event.getIteration(), statistics.getPercentile(99));
		q10History.put(event.getIteration(), statistics.getPercentile(10));
		minHistory.put(event.getIteration(), statistics.getMin());

		XYLineChart chart = new XYLineChart("Travel time convergence", "Iteration", "Relative Error");

		chart.addSeries("Mean", meanHistory);
		chart.addSeries("Median", medianHistory);
		chart.addSeries("10% Quantile", q10History);
		chart.addSeries("90% Quantile", q90History);
		chart.addSeries("99% Quantile", q99History);

		chart.saveAsPng(outputHierarchy.getOutputFilename("travel_time_error.png"), 800, 600);

		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputHierarchy.getOutputFilename("travel_time_error.csv"))));

			writer.write(String.join(";", new String[] { "iteration", "mean", "median", "q10", "q90", "q99", "min", "max" })
					+ "\n");

			for (int iteration = 0; iteration < event.getIteration(); iteration++) {
				if (meanHistory.containsKey(iteration)) {
					writer.write(String.join(";", new String[] { //
							String.valueOf(iteration), //
							String.valueOf(meanHistory.get(iteration)), //
							String.valueOf(medianHistory.get(iteration)), //
							String.valueOf(q10History.get(iteration)), //
							String.valueOf(q90History.get(iteration)), //
							String.valueOf(q99History.get(iteration)), //
							String.valueOf(minHistory.get(iteration)), //
							String.valueOf(maxHistory.get(iteration)) //
					}) + "\n");
				}
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (Math.abs(statistics.getPercentile(99)) <= convergenceThreshold) {
			if (Math.abs(statistics.getMean()) <= convergenceThreshold) {
				isConverged = true;
			}
		}

		statistics.clear();
		departureEvents.clear();
		elementIndices.clear();

		if (detailedWriter != null) {
			try {
				detailedWriter.close();
				detailedWriter = null;
			} catch (IOException e) {
			}
		}
	}

	public boolean isConverged() {
		return isConverged;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() % detailedAnalysisInterval == 0 && detailedAnalysisInterval > 0) {
			String path = outputHierarchy.getIterationFilename(event.getIteration(), "travel_time_comparision.csv");
			detailedWriter = IOUtils.getBufferedWriter(path);

			try {
				detailedWriter.write(String.join(";", Arrays.asList("person", "index", "planned", "simulated")) + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
