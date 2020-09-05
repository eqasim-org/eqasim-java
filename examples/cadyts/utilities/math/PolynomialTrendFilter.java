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

import java.io.Serializable;

/**
 * 
 * Tracks a signal trend with an arbitrary-degree polynomial.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class PolynomialTrendFilter implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private final Regression regr;

	private int it = 0;

	// -------------------- CONSTRUCTION --------------------

	public PolynomialTrendFilter(final double lambda, final int degree) {
		this.regr = new Regression(lambda, 1 + degree);
	}

	// -------------------- IMPLEMENTATION --------------------

	private Vector x(final int it) {
		final Vector result = new Vector(this.regr.getDimension());
		for (int i = 0; i < result.size(); i++) {
			result.set(i, Math.pow(it, i));
		}
		return result;
	}

	public void setLambda(final double lambda) {
		this.regr.setInertia(lambda);
	}

	public void add(final double val) {
		this.regr.update(x(this.it++), val);
	}

	public double predict(final int steps) {
		return this.regr.predict(x(this.it - 1 + steps));
	}
}
