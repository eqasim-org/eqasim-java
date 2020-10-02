package org.eqasim.projects.astra16.travel_time;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.charts.XYLineChart;

public class TravelTimeComparisonListener
		implements PersonDepartureEventHandler, PersonArrivalEventHandler, IterationEndsListener {
	private final Population population;
	private final OutputDirectoryHierarchy outputHierarchy;

	private final DescriptiveStatistics statistics = new DescriptiveStatistics();

	private final Map<Integer, Double> meanHistory = new HashMap<>();
	private final Map<Integer, Double> medianHistory = new HashMap<>();
	private final Map<Integer, Double> stdHistory = new HashMap<>();
	private final Map<Integer, Double> q90History = new HashMap<>();

	private final Map<Id<Person>, PersonDepartureEvent> departureEvents = new HashMap<>();
	private final Map<Id<Person>, Integer> elementIndices = new HashMap<>();

	public TravelTimeComparisonListener(OutputDirectoryHierarchy outputHierarchy, Population population) {
		this.population = population;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!event.getPersonId().toString().startsWith("av:")) {
			if (event.getLegMode().equals(TransportMode.car)) {
				departureEvents.put(event.getPersonId(), event);
			}
		}
	}

	private Leg getNextLeg(Id<Person> personId) {
		int index = elementIndices.getOrDefault(personId, 0);

		List<? extends PlanElement> elements = population.getPersons().get(personId).getSelectedPlan()
				.getPlanElements();

		while (index < elements.size()) {
			PlanElement element = elements.get(index);

			if (element instanceof Leg) {
				elementIndices.put(personId, index + 1);
				return (Leg) element;
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
				double simulatedTravelTime = event.getTime() - departureTime;

				Leg leg = getNextLeg(event.getPersonId());

				Boolean isNew = (Boolean) leg.getAttributes().getAttribute("isNew");

				if (isNew != null && isNew) {
					leg.getAttributes().removeAttribute("isNew");
					double plannedTravelTime = leg.getTravelTime();

					statistics.addValue(simulatedTravelTime - plannedTravelTime);
				}
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		meanHistory.put(event.getIteration(), statistics.getMean());
		medianHistory.put(event.getIteration(), statistics.getPercentile(50));
		stdHistory.put(event.getIteration(), statistics.getStandardDeviation());
		q90History.put(event.getIteration(), statistics.getPercentile(90));

		XYLineChart chart = new XYLineChart("Travel time prediction", "Iteration", "Error");

		chart.addSeries("Mean", meanHistory);
		chart.addSeries("Median", medianHistory);
		chart.addSeries("90% Quantile", q90History);
		chart.addSeries("Stanard deviation", stdHistory);

		chart.saveAsPng(outputHierarchy.getOutputFilename("travel_time_error.png"), 800, 600);

		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputHierarchy.getOutputFilename("travel_time_error.csv"))));

			writer.write(String.join(";", new String[] { "iteration", "mean", "median", "q90", "std" }) + "\n");

			for (int iteration = 0; iteration < event.getIteration(); iteration++) {
				if (meanHistory.containsKey(iteration)) {
					writer.write(String.join(";", new String[] { //
							String.valueOf(iteration), //
							String.valueOf(meanHistory.get(iteration)), //
							String.valueOf(medianHistory.get(iteration)), //
							String.valueOf(q90History.get(iteration)), //
							String.valueOf(stdHistory.get(iteration)) //
					}) + "\n");
				}
			}

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		statistics.clear();
		departureEvents.clear();
		elementIndices.clear();
	}
}
