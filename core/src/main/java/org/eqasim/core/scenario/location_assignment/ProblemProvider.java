package org.eqasim.core.scenario.location_assignment;

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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.PopulationUtils;

class ProblemProvider
		implements MATSimDistanceSamplerProvider, MATSimDiscretizationThresholdProvider, MATSimDiscretizerProvider {
	final private DistanceSamplerFactory distanceSamplerFactory;
	final private FacilityTypeDiscretizerFactory discretizerFactory;
	final private Map<String, Double> discretizationThresholds;

	public ProblemProvider(DistanceSamplerFactory distanceSamplerFactory,
			FacilityTypeDiscretizerFactory discretizerFactory, Map<String, Double> discretizationThresholds) {
		this.distanceSamplerFactory = distanceSamplerFactory;
		this.discretizerFactory = discretizerFactory;
		this.discretizationThresholds = discretizationThresholds;
	}

	@Override
	public List<Discretizer> getDiscretizers(MATSimAssignmentProblem problem) {
		return problem.getChainActivities().stream().map(Activity::getType).map(discretizerFactory::createDiscretizer)
				.collect(Collectors.toList());
	}

	@Override
	public List<Double> getDiscretizationThresholds(MATSimAssignmentProblem problem) {
		return problem.getAllLegs().stream().map(Leg::getMode).map(discretizationThresholds::get)
				.collect(Collectors.toList());
	}

	@Override
	public List<DistanceSampler> getDistanceSamplers(MATSimAssignmentProblem problem) {
		return problem.getAllLegs().stream().map(leg -> {
			double duration = PopulationUtils.getNextActivity(problem.getPlan(), leg).getStartTime()
					- PopulationUtils.getPreviousActivity(problem.getPlan(), leg).getEndTime();
			return distanceSamplerFactory.createDistanceSampler(leg.getMode(), duration);
		}).collect(Collectors.toList());
	}
}
