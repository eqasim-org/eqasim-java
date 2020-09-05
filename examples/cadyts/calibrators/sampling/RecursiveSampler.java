/*
 * Cadyts - Calibration of dynamic traffic simulations
 *
 * Copyright 2009, 2010 Gunnar Flötteröd
 * 
 *
 * This file is part of Cadyts.
 *
 * Cadyts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cadyts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cadyts.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@epfl.ch
 *
 */ 
package cadyts.calibrators.sampling;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import cadyts.demand.Plan;
import cadyts.demand.PlanStep;
import cadyts.utilities.math.PolynomialTrendFilter;

/**
 * Implements a scaling of the prior plan choice distribution that is based on
 * sampling importance resampling.
 * 
 * TODO: Currently, the linear trend filter is only updated _after_ an accept.
 * The precision would improve if the update occurred even during the rejects!
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class RecursiveSampler<L> implements ChoiceSampler<L> {

	// -------------------- MEMBERS --------------------

	private final SamplingCalibrator<L> calibrator;

	private final PolynomialTrendFilter likelihoodTrendFilter;

	// runtime parameters

	private final Map<Iterable<PlanStep<L>>, Double> plan2linEffect = new HashMap<Iterable<PlanStep<L>>, Double>();

	private int draws = 0;

	private double likelihoodSum = 0;

	private boolean acceptNext = false;

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public RecursiveSampler(final SamplingCalibrator<L> calibrator) {
		// CHECK
		if (calibrator == null)
			throw new IllegalArgumentException("calibrator is null");
		// CONTINUE
		this.calibrator = calibrator;
		this.likelihoodTrendFilter = new PolynomialTrendFilter(calibrator
				.getRegressionInertia(), 1);
		this.init();
	}

	private void init() {
		this.likelihoodTrendFilter.setLambda(this.calibrator
				.getRegressionInertia());
		this.plan2linEffect.clear();
		this.draws = 0;
		this.likelihoodSum = 0;
		this.acceptNext = false;
	}

	// -------------------- BASIC FUNCTIONALITY --------------------

	public double getPredictedLikelihood() {
		return this.likelihoodTrendFilter.predict(1);
	}

	// for testing purposes
	PolynomialTrendFilter getLikelihoodTrendFilter() {
		return this.likelihoodTrendFilter;
	}

	// -------------------- ACCEPT/REJECT FUNCTIONALITY --------------------

	private double likelihood(final Plan<L> plan) {
		Double linEffect = this.plan2linEffect.get(plan);
		if (linEffect == null) {
			linEffect = this.calibrator.calcLinearPlanEffect(plan);
			this.plan2linEffect.put(plan, linEffect);
		}
		return Math.exp(linEffect);
	}

	@Override
	public void enforceNextAccept() {
		this.acceptNext = true;
	}

	/**
	 * Whenever an agent is about to make a choice, draw plans according to this
	 * agent's behavioral model (i.e., this agents prior choice distribution)
	 * until this function returns an "accept". The first accepted plan can be
	 * considered a draw from the agent's behavioral posterior distribution.
	 * <em>It is of greatest importance that the
	 * agent does indeed implement the first accepted plan!</em>
	 * 
	 * @param plan
	 *            the plan under consideration, must be a draw from the
	 *            behavioral prior distribution
	 * @return if the plan is accepted
	 */
	@Override
	public boolean isAccepted(final Plan<L> plan) {

		this.draws++;
		final double likelihood = this.likelihood(plan);
		final boolean infiniteLikelihood = Double.isInfinite(likelihood);
		if (!infiniteLikelihood) {
			this.likelihoodSum += likelihood;
		}

		final boolean isAccepted;
		if (this.acceptNext) {
			isAccepted = true;
		} else {
			if (infiniteLikelihood) {
				isAccepted = true;
				Logger.getLogger(this.getClass().getName()).warning(
						"infinite likelihood numerator");
			} else {
				final double pAccept = likelihood
						/ (likelihood + (this.calibrator.getMaxDraws() - this.draws)
								* this.likelihoodTrendFilter.predict(1));
				isAccepted = (this.calibrator.getRandom().nextDouble() < pAccept);
			}
		}

		if (isAccepted || (this.calibrator.getMaxDraws() == this.draws)) {
			if (!isAccepted) {
				Logger.getLogger(this.getClass().getName()).warning(
						"no accept after maximum number of draws");
			}
			if (infiniteLikelihood) {
				this.draws--;
			}
			if (this.draws > 0) {
				this.likelihoodTrendFilter.add(this.likelihoodSum / this.draws);
			}
			this.calibrator.addToDemand(plan);
			this.init();
		}

		return isAccepted;
	}
}
