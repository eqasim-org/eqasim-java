package org.eqasim.core.scenario.location_assignment;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eqasim.core.location_assignment.algorithms.DistanceSampler;
import org.eqasim.core.location_assignment.algorithms.discretizer.Discretizer;
import org.eqasim.core.location_assignment.matsim.MATSimAssignmentProblem;
import org.eqasim.core.location_assignment.matsim.discretizer.FacilityTypeDiscretizerFactory;
import org.eqasim.core.location_assignment.matsim.setup.MATSimDiscretizationThresholdProvider;
import org.eqasim.core.location_assignment.matsim.setup.MATSimDiscretizerProvider;
import org.eqasim.core.location_assignment.matsim.setup.MATSimDistanceSamplerProvider;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;

class ProblemProvider
		implements MATSimDistanceSamplerProvider, MATSimDiscretizationThresholdProvider, MATSimDiscretizerProvider {
	final private DistanceSamplerFactory distanceSamplerFactory;
	final private FacilityTypeDiscretizerFactory discretizerFactory;
	final private Map<String, Double> discretizationThresholds;
	final private boolean requirePtAccessibility;

	public ProblemProvider(DistanceSamplerFactory distanceSamplerFactory,
			FacilityTypeDiscretizerFactory discretizerFactory, Map<String, Double> discretizationThresholds,
			boolean requirePtAccessibility) {
		this.distanceSamplerFactory = distanceSamplerFactory;
		this.discretizerFactory = discretizerFactory;
		this.discretizationThresholds = discretizationThresholds;
		this.requirePtAccessibility = requirePtAccessibility;
	}

	@Override
	public List<Discretizer> getDiscretizers(MATSimAssignmentProblem problem) {
		List<Discretizer> discretizers = new LinkedList<>();

		for (int index = 0; index < problem.getChainActivities().size(); index++) {
			if (!requirePtAccessibility)
				discretizers.add(
						discretizerFactory.createDiscretizer(problem.getChainActivities().get(index).getType(), false));
			else {
				String activityType = problem.getChainActivities().get(index).getType();
				String mode = problem.getAllLegs().get(index).getMode();

				if (mode.equals(TransportMode.pt)) {
					discretizers.add(discretizerFactory.createDiscretizer(activityType, true));
				} else {
					discretizers.add(discretizerFactory.createDiscretizer(activityType, false));
				}
			}
		}

		return discretizers;
	}

	@Override
	public List<Double> getDiscretizationThresholds(MATSimAssignmentProblem problem) {
		return problem.getAllLegs().stream().map(Leg::getMode).map(discretizationThresholds::get)
				.collect(Collectors.toList());
	}

	@Override
	public List<DistanceSampler> getDistanceSamplers(MATSimAssignmentProblem problem) {
		return problem.getAllLegs().stream().map(leg -> {
			double duration = PopulationUtils.getNextActivity(problem.getPlan(), leg).getStartTime().seconds()
					- PopulationUtils.getPreviousActivity(problem.getPlan(), leg).getEndTime().seconds();
			return distanceSamplerFactory.createDistanceSampler(leg.getMode(), duration);
		}).collect(Collectors.toList());
	}
}
