package org.eqasim.core.location_assignment.matsim.setup;

import java.util.List;

import org.eqasim.core.location_assignment.algorithms.ThresholdObjectiveFunction.DiscretizationThresholdProvider;
import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.matsim.MATSimAssignmentProblem;

public interface MATSimDiscretizationThresholdProvider {
	List<Double> getDiscretizationThresholds(MATSimAssignmentProblem problem);

	static public class Adapter implements DiscretizationThresholdProvider {
		final private MATSimDiscretizationThresholdProvider delegate;

		public Adapter(MATSimDiscretizationThresholdProvider delegate) {
			this.delegate = delegate;
		}

		@Override
		public List<Double> getDiscretizationThresholds(LocationAssignmentProblem problem) {
			return delegate.getDiscretizationThresholds((MATSimAssignmentProblem) problem);
		}
	}
}
