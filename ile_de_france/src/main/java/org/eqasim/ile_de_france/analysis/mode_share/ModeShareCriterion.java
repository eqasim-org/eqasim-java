package org.eqasim.ile_de_france.analysis.mode_share;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.analysis.AnalysisOutputListener;
import org.eqasim.ile_de_france.travel_time.TravelTimeComparisonListener;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

public class ModeShareCriterion implements IterationEndsListener, TerminationCriterion {
	public static int convergenceIteration = Integer.MAX_VALUE;

	private final Population population;
	private final MainModeIdentifier mainModeIdentifier;
	private final OutputDirectoryHierarchy outputHierarchy;

	private final Map<Id<Person>, Integer> personHashes = new HashMap<>();

	private final List<Double> updatedCounts = new LinkedList<>();
	private final List<Double> totals = new LinkedList<>();
	private final List<Double> updatedShares = new LinkedList<>();
	private final List<Double> correctedShares = new LinkedList<>();

	private final double dmcProbability;
	private final double convergenceThreshold;

	private final TravelTimeComparisonListener travelTimeListener;

	public ModeShareCriterion(MainModeIdentifier mainModeIdentifier, Population population,
			OutputDirectoryHierarchy outputHierarchy, double dmcProbability, double convergenceThreshold,
			TravelTimeComparisonListener travelTimeListener) {
		this.population = population;
		this.mainModeIdentifier = mainModeIdentifier;
		this.outputHierarchy = outputHierarchy;
		this.dmcProbability = dmcProbability;
		this.convergenceThreshold = convergenceThreshold;
		this.travelTimeListener = travelTimeListener;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		Map<Id<Person>, Integer> updatedHashes = new HashMap<>();

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			int hash = person.getId().hashCode();

			int index = 1;

			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
				String mode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
				hash += index * mode.hashCode();
				index++;
			}

			updatedHashes.put(person.getId(), hash);
		}

		if (event.getIteration() > 0) {
			int updatedPersons = 0;
			int totalPersons = 0;

			for (Map.Entry<Id<Person>, Integer> entry : updatedHashes.entrySet()) {
				if (!personHashes.get(entry.getKey()).equals(entry.getValue())) {
					updatedPersons++;
				}

				totalPersons++;
			}

			updatedCounts.add((double) updatedPersons);
			totals.add((double) totalPersons);

			double currentUpdatedShare = (double) updatedPersons / (double) totalPersons;
			updatedShares.add(currentUpdatedShare);

			double currentCorrectedShare = currentUpdatedShare / dmcProbability;
			correctedShares.add(currentCorrectedShare);

			if (currentCorrectedShare < convergenceThreshold && convergenceIteration == Integer.MAX_VALUE
					&& travelTimeListener.isConverged()) {
				convergenceIteration = event.getIteration() + 1;
				AnalysisOutputListener.convergenceIteration = event.getIteration() + 1;
			}

			// Writing

			try {
				BufferedWriter writer = IOUtils
						.getBufferedWriter(outputHierarchy.getOutputFilename("mode_share_convergence.csv"));

				writer.write(String.join(";", new String[] { //
						"iteration", "updated_count", "total_count", "updated_share", "corrected_share" //
				}) + "\n");

				for (int i = 0; i < totals.size(); i++) {
					writer.write(String.join(";", new String[] { //
							String.valueOf(i + 1), //
							String.valueOf(updatedCounts.get(i)), //
							String.valueOf(totals.get(i)), //
							String.valueOf(updatedShares.get(i)), //
							String.valueOf(correctedShares.get(i)) //
					}) + "\n");
				}

				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Plotting

			Map<Integer, Double> history = new HashMap<>();

			for (int i = 0; i < correctedShares.size(); i++) {
				history.put(i, correctedShares.get(i));
			}

			XYLineChart chart = new XYLineChart("Mode share convergence", "Iteration", "Corr. share");
			chart.addSeries("Corr. share", history);
			chart.saveAsPng(outputHierarchy.getOutputFilename("mode_share_convergence.png"), 800, 600);
		}

		personHashes.putAll(updatedHashes);
	}

	@Override
	public boolean continueIterations(int iteration) {
		return iteration <= convergenceIteration;
	}
}
