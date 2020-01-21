package org.eqasim.projects.dynamic_av.waiting_time;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.facilities.Facility;

import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.waiting_time.WaitingTime;

public class WaitingTimeComparisonListener
		implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, IterationEndsListener {
	private final WaitingTime waitingTime;
	private final OutputDirectoryHierarchy outputHierarchy;

	private final DescriptiveStatistics statistics = new DescriptiveStatistics();

	private final Map<Integer, Double> meanHistory = new HashMap<>();
	private final Map<Integer, Double> medianHistory = new HashMap<>();
	private final Map<Integer, Double> stdHistory = new HashMap<>();
	private final Map<Integer, Double> q90History = new HashMap<>();

	private final Map<Id<Person>, PersonDepartureEvent> departureEvents = new HashMap<>();

	public WaitingTimeComparisonListener(OutputDirectoryHierarchy outputHierarchy, WaitingTime waitingTime) {
		this.waitingTime = waitingTime;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(AVModule.AV_MODE)) {
			departureEvents.put(event.getPersonId(), event);
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (event.getVehicleId().toString().startsWith("av:")) {
			if (!event.getPersonId().toString().startsWith("av:")) {
				PersonDepartureEvent departureEvent = departureEvents.remove(event.getPersonId());

				double departureTime = departureEvent.getTime();
				double simulatedWaitingTime = event.getTime() - departureTime;

				Facility wrapper = new Facility() {
					@Override
					public Coord getCoord() {
						return null;
					}

					@Override
					public Map<String, Object> getCustomAttributes() {
						return null;
					}

					@Override
					public Id<Link> getLinkId() {
						return departureEvent.getLinkId();
					}
				};

				double estimatedWaitingTime = waitingTime.getWaitingTime(wrapper, departureTime);
				statistics.addValue(simulatedWaitingTime - estimatedWaitingTime);
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		meanHistory.put(event.getIteration(), statistics.getMean());
		medianHistory.put(event.getIteration(), statistics.getPercentile(50));
		stdHistory.put(event.getIteration(), statistics.getStandardDeviation());
		q90History.put(event.getIteration(), statistics.getPercentile(90));

		XYLineChart chart = new XYLineChart("Waiting time prediction", "Iteration", "Error");

		chart.addSeries("Mean", meanHistory);
		chart.addSeries("Median", medianHistory);
		chart.addSeries("90% Quantile", q90History);
		chart.addSeries("Stanard deviation", stdHistory);

		chart.saveAsPng(outputHierarchy.getOutputFilename("waiting_time_error.png"), 800, 600);

		statistics.clear();
		departureEvents.clear();
	}
}
