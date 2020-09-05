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

import cadyts.calibrators.analytical.ChoiceParameterCalibrator;
import cadyts.demand.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MATSimChoiceParameterCalibrator<L> extends
		ChoiceParameterCalibrator<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- CONSTRUCTION --------------------

	public MATSimChoiceParameterCalibrator(final Random rnd,
			final double regressionInertia, final int parameterDimension) {
		super("calibration-log.txt", rnd.nextLong(), 3600, parameterDimension);
		this.setRegressionInertia(regressionInertia);
	}

	public MATSimChoiceParameterCalibrator(final String logFile,
			final Random rnd, final double regressionInertia, final int parameterDimension) {
		super(logFile, rnd.nextLong(), 3600, parameterDimension);
		this.setRegressionInertia(regressionInertia);
	}

	// -------------------- MATSIM-SPECIFICS --------------------

	public double getUtilityCorrection(final Plan<L> plan) {
		return super.calcLinearPlanEffect(plan);
	}
}
