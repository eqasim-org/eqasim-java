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

import static java.lang.Math.sqrt;

/**
 * Calculates Cholesky decomposition and inverse of a symmetric positive
 * definite Matrix.
 * 
 * @see T. Seaks. SYMINV: An algorithm for the inverse of a positive definite
 *      matrix by the Cholesky decomposition. Econometrica 40(5), 1972.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Cholesky {

	// -------------------- MEMBERS --------------------

	private Matrix result = null;

	// -------------------- CONSTRUCTION --------------------

	public Cholesky() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public boolean valid() {
		return (this.result != null);
	}

	public Matrix getResult() {
		return this.result;
	}

	private void checkNonNull(final Matrix _A) {
		if (_A == null) {
			this.result = null;
			throw new IllegalArgumentException("matrix must not be null");
		}
	}

	private void checkQuadratic(final Matrix _A) {
		if (_A.rowSize() != _A.columnSize()) {
			this.result = null;
			throw new IllegalArgumentException("this is not a quadratic matrix");
		}
	}

	/**
	 * Calculates the square root L of the symmetric positive matrix A. L is a
	 * lower triangular matrix with A=LL'.
	 * 
	 * @return true if the calculation was successful, false otherwise
	 */
	public boolean calculateSquareRoot(final Matrix _A) {

		this.checkNonNull(_A);
		this.checkQuadratic(_A);
		this.result = new Matrix(_A.rowSize(), _A.columnSize());

		for (int i = 0; i < this.result.rowSize(); i++) {
			final Vector result_i = this.result.getRow(i);
			final Vector _A_i = _A.getRow(i);
			for (int j = 0; j <= i; j++) {
				final Vector result_j = this.result.getRow(j);
				double s = _A_i.get(j);
				for (int k = 0; k < j; k++) {
					s -= result_i.get(k) * result_j.get(k);
				}
				if (j < i) {
					result_i.set(j, s / result_j.get(j));
				} else { // (j == i)
					if (s <= 0) {
						this.result = null;
						return this.valid();
					}
					result_i.set(i, sqrt(s));
				}
			}
		}

		return this.valid();
	}

	/**
	 * Entirely unchecked implementation. Used only internally such that
	 * invertibility of _L is guaranteed by the calling function. Package
	 * private so that it can be unit-tested.
	 */
	Matrix invertLowerTriangular(final Matrix _L) {
		final Matrix _S = new Matrix(_L.rowSize(), _L.columnSize());
		for (int i = 0; i < _S.rowSize(); i++) {
			final Vector _L_i = _L.getRow(i);
			final double _L_ii = _L_i.get(i);
			final Vector _S_i = _S.getRow(i);
			_S_i.set(i, 1.0 / _L_ii);
			for (int j = 0; j < i; j++) {
				double s = 0;
				for (int k = 0; k < i; k++) {
					s += _L_i.get(k) * _S.getRow(k).get(j);
				}
				_S_i.set(j, -s / _L_ii);
			}
		}
		return _S;
	}

	/**
	 * Inverts the symmetric positive definite matrix _A.
	 */
	public boolean invert(final Matrix _A) {
		if (this.calculateSquareRoot(_A)) {
			final Matrix _L_inv = this.invertLowerTriangular(this.result);
			this.result = Matrix.product(_L_inv.newTransposed(), _L_inv);
		}
		return this.valid();
	}

}
