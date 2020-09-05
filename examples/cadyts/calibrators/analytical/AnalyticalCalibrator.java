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
package cadyts.calibrators.analytical;

import static cadyts.utilities.math.MathHelpers.draw;
import static java.lang.Math.exp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import cadyts.calibrators.Calibrator;
import cadyts.demand.Plan;
import cadyts.utilities.math.Vector;

/**
 * A Calibrator that is applicable to analytical demand simulators that
 * enumerate choice sets and provide a choice probability for every element of
 * every choice set.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <L>
 *            the network link type
 */
public class AnalyticalCalibrator<L> extends Calibrator<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	protected static final double MIN_PROB = 1e-6;

	public static final boolean DEFAULT_BRUTE_FORCE = false;

	// -------------------- MEMBERS --------------------

	private Vector lastChoiceProbs;

	private int lastChoiceIndex;

	private boolean bruteForce = DEFAULT_BRUTE_FORCE;

	// -------------------- CONSTRUCTION --------------------

	public AnalyticalCalibrator(final String logFile, final Long randomSeed,
			final int timeBinSize_s) {
		super(logFile, randomSeed, timeBinSize_s);
		Logger.getLogger(this.myName).info(
				"default bruteForce is " + this.bruteForce);
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setBruteForce(final boolean bruteForce) {
		this.bruteForce = bruteForce;
		Logger.getLogger(myName).info("set bruteForce to " + this.bruteForce);
	}

	public boolean getBruteForce() {
		return this.bruteForce;
	}

	// -------------------- RESULT ACCESS --------------------

	public double getLastChoiceProb(final int index) {
		return this.lastChoiceProbs.get(index);
	}

	public int getLastChoiceIndex() {
		return this.lastChoiceIndex;
	}

	// -------------------- IMPLEMENTATION --------------------

	public int selectPlan(final List<? extends Plan<L>> plans,
			final Vector choiceProbs) {
		return this.selectPlan(null, plans, choiceProbs);
	}

	protected int selectPlan(final Integer overrideChoice,
			final List<? extends Plan<L>> plans, final Vector choiceProbs) {

		// CHECK

		if (plans == null) {
			throw new IllegalArgumentException("plans list ist null");
		}
		if (plans.size() == 0) {
			throw new IllegalArgumentException("plans list is empty");
		}
		if (choiceProbs == null) {
			throw new IllegalArgumentException(
					"choice probability list is null");
		}
		if (choiceProbs.size() == 0) {
			throw new IllegalArgumentException(
					"choice probability list is empty");
		}
		if (plans.size() != choiceProbs.size()) {
			throw new IllegalArgumentException("plans list has " + plans.size()
					+ " elements, but choice probability list has "
					+ choiceProbs.size() + " elements");
		}
		if (overrideChoice != null
				&& (overrideChoice < 0 || overrideChoice >= plans.size())) {
			throw new IllegalArgumentException("overriding choice "
					+ overrideChoice + "is not in {0,...," + (plans.size() - 1)
					+ "}");
		}

		// CONTINUE

		if (this.bruteForce) {
			final List<Integer> bestIndices = new ArrayList<Integer>(plans
					.size());
			double bestLambda = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < plans.size(); i++) {
				final double lambda = this.calcLinearPlanEffect(plans.get(i));
				if (lambda > bestLambda) {
					bestIndices.clear();
					bestIndices.add(i);
					bestLambda = lambda;
				} else if (lambda == bestLambda) {
					bestIndices.add(i);
				}
			}
			this.lastChoiceProbs = new Vector(plans.size());
			for (Integer index : bestIndices) {
				this.lastChoiceProbs.set(index, Math.max(MIN_PROB, choiceProbs
						.get(index)));
			}
		} else {
			this.lastChoiceProbs = choiceProbs.copy();
			final Vector lambdas = new Vector(plans.size());
			for (int i = 0; i < plans.size(); i++) {
				lambdas.set(i, this.calcLinearPlanEffect(plans.get(i)));
			}
			final double maxLambda = lambdas.max();
			for (int i = 0; i < plans.size(); i++) {
				this.lastChoiceProbs.mult(i, exp(lambdas.get(i) - maxLambda));
				this.lastChoiceProbs.set(i, Math.max(MIN_PROB,
						this.lastChoiceProbs.get(i)));
			}
		}
		this.lastChoiceProbs.mult(1.0 / this.lastChoiceProbs.sum());

		if (overrideChoice == null) {
			this.lastChoiceIndex = draw(this.lastChoiceProbs, this.getRandom());
		} else {
			this.lastChoiceIndex = overrideChoice;
		}

		this.addToDemand(plans.get(this.getLastChoiceIndex()));
		return this.getLastChoiceIndex();
	}
}
