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
package cadyts.utilities.math;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class CholeskyModified extends Cholesky {

	// -------------------- CONSTANTS --------------------

	private final int maxTrials;

	// -------------------- MEMBERS --------------------

	private int trials = 0;

	// -------------------- CONSTRUCTOR --------------------

	public CholeskyModified(final int maxTrials) {
		super();
		this.maxTrials = maxTrials;
	}

	// -------------------- GETTERS --------------------

	public int getTrials() {
		return this.trials;
	}

	public int getMaxTrials() {
		return this.maxTrials;
	}

	// -------------------- IMPLEMENTATION --------------------

	private double smallestDiagonalElement(final Matrix _A) {
		double result = Double.POSITIVE_INFINITY;
		for (int i = 0; i < _A.rowSize(); i++) {
			result = Math.min(result, _A.getRow(i).get(i));
		}
		return result;
	}

	@Override
	public boolean calculateSquareRoot(final Matrix _A) {

		this.trials = 1;
		double tau = 0;
		Matrix _B = _A.copy();

		final double frobNorm = _A.frobeniusNorm();
		if (smallestDiagonalElement(_A) <= 0) {
			tau = 0.5 * frobNorm;
			_B.add(Matrix.newDiagonal(_B.rowSize(), 1.0), tau);
		}

		while (!super.calculateSquareRoot(_B) && this.trials < this.maxTrials) {
			tau = Math.max(2.0 * tau, 0.5 * frobNorm);
			_B = _A.copy();
			_B.add(Matrix.newDiagonal(_B.rowSize(), 1.0), tau);
			this.trials++;
		}

		return this.valid();
	}
}
