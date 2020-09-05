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
package cadyts.interfaces.dracula;

import static cadyts.utilities.math.MathHelpers.round;

import java.util.List;
import java.util.logging.Logger;

import cadyts.demand.PlanChoiceModel;
import cadyts.utilities.math.MultinomialLogit;
import cadyts.utilities.math.Vector;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaPlanChoiceModel implements PlanChoiceModel<DraculaPlan> {

	// -------------------- MEMBERS --------------------

	private final DraculaCalibrator calibrator;

	private final double stayAtHomeProb;

	// -------------------- CONSTRUCTION --------------------

	DraculaPlanChoiceModel(final DraculaCalibrator calibrator) {
		if (calibrator == null) {
			throw new IllegalArgumentException("calibrator is null");
		}
		this.calibrator = calibrator;
		this.stayAtHomeProb = (calibrator.getDemandScale() - 1.0)
				/ calibrator.getDemandScale();
	}

	// --------------- IMPLEMENTATION OF PlanChoiceModel ---------------

	@Override
	public Vector getChoiceProbabilities(List<? extends DraculaPlan> plans) {
		/*
		 * (1) identify choice probabilities for all non-stay-at-home plans
		 */
		final MultinomialLogit mnl = new MultinomialLogit(plans.size() - 1, 1);
		mnl.setCoefficient(0, this.calibrator.getBetaTT_s());
		for (int i = 0; i < plans.size() - 1; i++) {
			final DraculaPlan plan = plans.get(i);
			if (plan.isStayAtHome()) {
				Logger.getLogger(this.getClass().getName()).warning(
						"encountered stay-at-home plan "
								+ "before last position in plan list");
			}
			double tt_s = 0;
			for (DraculaLink link : plan.getRoute().getLinks()) {
				tt_s += this.calibrator.getTravelTimes().getTT_s(link,
						plan.getDepartureTime_s() + round(tt_s));
			}
			mnl.setAttribute(i, 0, tt_s);
		}
		/*
		 * (2) scale down real choice probs and add stay-at-home alternative
		 */
		final DraculaPlan lastPlan = plans.get(plans.size() - 1);
		if (!lastPlan.isStayAtHome()) {
			Logger.getLogger(this.getClass().getName()).warning(
					"last plan in list is no stay-at-home plan");
		}
		final Vector result = mnl.getProbs().copyEnlarged(1);
		result.mult(1.0 - this.stayAtHomeProb);
		result.set(result.size() - 1, this.stayAtHomeProb);
		return result;
	}
}
