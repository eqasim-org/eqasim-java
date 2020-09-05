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
package cadyts.interfaces.matsim;

import java.util.Random;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.demand.Plan;

/**
 * 
 * This calibrator affects the MATSim choice distributions through utility
 * corrections. For this, do the following in every iteration for every
 * replanning agent:
 * 
 * (1) obtain the utility correction for every plan in the considered agent's
 * choice set with getUtilityCorrection(..);
 * 
 * (2) let MATSim select a plan based on the modified utilities;
 * 
 * (3) indicate the selectedPlan with registerChoice(..).
 * 
 * Apart from this, the usual rules for calibration hold: configuration and
 * measurement registration must be done before the iterations, and after every
 * network loading a call to afterNetworkLoading(..) is necessary.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MATSimUtilityModificationCalibrator<L> extends
		AnalyticalCalibrator<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- CONSTRUCTION --------------------

	public MATSimUtilityModificationCalibrator(final Random rnd,
			final double regressionInertia) {
		super("calibration-log.txt", rnd.nextLong(), 3600);
		this.setRegressionInertia(regressionInertia);
	}

	public MATSimUtilityModificationCalibrator(final String logFile,
			final Random rnd, final double regressionInertia) {
		super(logFile, rnd.nextLong(), 3600);
		this.setRegressionInertia(regressionInertia);
	}

	// -------------------- MATSIM-SPECIFICS --------------------

	/**
	 * Call this before each replanning to evaluate all plans of every
	 * considered agent.
	 */
	public double getUtilityCorrection(final Plan<L> plan) {
		return super.calcLinearPlanEffect(plan);
	}

	/**
	 * Call this before each network loading to indicate the selected plan of
	 * every considered agent.
	 */
	public void registerChoice(final Plan<L> plan) {
		super.addToDemand(plan);
	}

}
