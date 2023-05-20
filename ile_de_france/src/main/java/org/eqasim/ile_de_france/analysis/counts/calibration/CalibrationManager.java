package org.eqasim.ile_de_france.analysis.counts.calibration;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.ile_de_france.analysis.counts.DailyCounts;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;

public class CalibrationManager {
	private final DailyCounts reference;
	private final Network network;
	private final double sampleSize;

	private final double threshold = 5.0 * 60.0;
	private final double minimumCapacity = 600.0;
	private final double maximumCapacity = 7200.0;

	private final double adaptationFactor = 0.05;

	public CalibrationManager(DailyCounts reference, Network network, double sampleSize) {
		this.reference = reference;
		this.network = network;
		this.sampleSize = sampleSize;
	}

	public CalibrationUpdate update(int iteration, Map<Id<Link>, List<Integer>> counts) {
		CalibrationUpdate update = new CalibrationUpdate();
		update.iteration = iteration;

		// I) Correction factor

		double cumulativeReference = 0.0;
		double cumulativeSimulation = 0.0;

		for (var entry : reference.getCounts().entrySet()) {
			cumulativeReference += entry.getValue();
			cumulativeSimulation += counts.get(entry.getKey()).stream().mapToDouble(i -> i).sum() / sampleSize;
		}

		double correctionFactor = cumulativeReference / cumulativeSimulation;
		update.correctionFactor = correctionFactor;

		// Error metrics for information

		List<Double> errors = new LinkedList<>();

		for (var entry : reference.getCounts().entrySet()) {
			double referenceValue = entry.getValue();
			double simulationValue = correctionFactor * counts.get(entry.getKey()).stream().mapToDouble(i -> i).sum()
					/ sampleSize;
			errors.add(simulationValue - referenceValue);
		}

		update.rmse = Math.sqrt(errors.stream().mapToDouble(d -> Math.pow(d, 2.0)).sum() / errors.size());
		update.mae = errors.stream().mapToDouble(Math::abs).sum() / errors.size();

		// Adjust capacities

		for (var entry : reference.getCounts().entrySet()) {
			CalibrationUpdate.LinkItem item = new CalibrationUpdate.LinkItem();

			Id<Link> linkId = entry.getKey();
			item.linkId = linkId;

			double referenceValue = entry.getValue();
			item.referenceCount = referenceValue;

			double currentValue = counts.get(linkId).stream().mapToDouble(i -> i).sum();
			item.simulationCount = currentValue;

			currentValue /= sampleSize;
			item.scaledCount = currentValue;

			currentValue *= correctionFactor;
			item.correctedCount = currentValue;

			Link link = network.getLinks().get(linkId);
			String osm = (String) link.getAttributes().getAttribute("osm:highway");

			item.currentCapacity = link.getCapacity();

			Double initialCapacity = (Double) link.getAttributes().getAttribute("initial_capacity");
			if (initialCapacity == null) {
				initialCapacity = link.getCapacity();
				link.getAttributes().putAttribute("initial_capacity", initialCapacity);
			}

			item.initialCapacity = initialCapacity;

			Set<Link> adaptedLinks = new HashSet<>();

			{
				// Forward
				IdSet<Link> visited = new IdSet<>(Link.class);
				List<Tuple<Link, Double>> queue = new LinkedList<>();
				queue.add(Tuple.of(link, 0.0));

				while (queue.size() > 0) {
					var currentEntry = queue.remove(0);

					Link currentLink = currentEntry.getFirst();
					double currentTravelTime = currentEntry.getSecond();

					visited.add(currentLink.getId());

					for (Link candidateLink : currentLink.getToNode().getOutLinks().values()) {
						double candidateTravelTime = candidateLink.getLength() / candidateLink.getFreespeed();

						if (!visited.contains(candidateLink.getId())
								&& currentTravelTime + candidateTravelTime < threshold) {
							String candidateOsm = (String) link.getAttributes().getAttribute("osm:highway");

							if (osm.equals(candidateOsm)) {
								adaptedLinks.add(candidateLink);
								queue.add(Tuple.of(candidateLink, currentTravelTime + candidateTravelTime));
							}
						}
					}
				}
			}

			{
				// Backward
				IdSet<Link> visited = new IdSet<>(Link.class);
				List<Tuple<Link, Double>> queue = new LinkedList<>();
				queue.add(Tuple.of(link, 0.0));

				while (queue.size() > 0) {
					var currentEntry = queue.remove(0);

					Link currentLink = currentEntry.getFirst();
					double currentTravelTime = currentEntry.getSecond();

					visited.add(currentLink.getId());

					for (Link candidateLink : currentLink.getFromNode().getInLinks().values()) {
						double candidateTravelTime = candidateLink.getLength() / candidateLink.getFreespeed();

						if (!visited.contains(candidateLink.getId())
								&& currentTravelTime + candidateTravelTime < threshold) {
							String candidateOsm = (String) link.getAttributes().getAttribute("osm:highway");

							if (osm.equals(candidateOsm)) {
								adaptedLinks.add(candidateLink);
								queue.add(Tuple.of(candidateLink, currentTravelTime + candidateTravelTime));
							}
						}
					}
				}
			}

			// Now we have found all links to adapt

			double adaptation = 1.0;
			if (referenceValue < currentValue) {
				adaptation += adaptationFactor;
			} else {
				adaptation -= adaptationFactor;
			}

			for (Link adaptedLink : adaptedLinks) {
				adaptedLink.setCapacity(adaptedLink.getCapacity() * adaptation);

				if (adaptedLink.getCapacity() < minimumCapacity) {
					adaptedLink.setCapacity(minimumCapacity);
				}

				if (adaptedLink.getCapacity() > maximumCapacity) {
					adaptedLink.setCapacity(maximumCapacity);
				}
			}

			item.updatedLinks = adaptedLinks.size();
			item.updatedCapacity = link.getCapacity();

			update.links.add(item);
		}

		return update;
	}
}
