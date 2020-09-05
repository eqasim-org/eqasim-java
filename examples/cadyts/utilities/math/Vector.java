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
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 
 * Represents a vector.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Vector implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBERS --------------------

	private final double[] data;

	private final boolean immutable;

	// -------------------- CONSTRUCTION --------------------

	private Vector(final double[] data, final boolean immutable) {
		if (data == null) {
			throw new IllegalArgumentException("data must not be null");
		}
		if (data.length < 1) {
			throw new IllegalArgumentException(
					"dimension must be strictly positive");
		}
		this.data = data;
		this.immutable = immutable;
	}

	/**
	 * Creates a vector of dimension dim that is initialized with all zeros.
	 * 
	 * @param dim
	 *            the dimension of the vector, must be strictly positive
	 */
	public Vector(final int dim) {
		this(dim < 0 ? null : new double[dim], false);
	}

	/**
	 * 
	 * Creates a vector that is internally based on the data array. Changing
	 * data also changes the state of the Vector instance.
	 * 
	 * @param data
	 *            the data with which the vector is to be initialized, must not
	 *            be null and have at least one entry
	 */
	public Vector(final double... data) {
		this(data, false);
	}

	public Vector(final List<Double> data) {
		this(data == null ? null : new double[data.size()], false);
		if (data != null) {
			for (int i = 0; i < data.size(); i++) {
				this.data[i] = data.get(i);
			}
		}
	}

	/**
	 * Creates a new dim-dimensional vector that is initialized with independent
	 * standard normal values (expectation of zero, variance of one). If the rnd
	 * parameter is null, the vector is initialized with based on an internally
	 * generated Random instance.
	 * 
	 * @param dim
	 *            the dimension of the newly created vector; must be strictly
	 *            positive
	 * @param rnd
	 *            a Random instance that is used to fill the Vector with random
	 *            numbers; may be null
	 * 
	 * @return a dim-dimensional Vector that is initialized with independent
	 *         standard normal values
	 */
	public static Vector newGaussian(final int dim, Random rnd) {
		if (rnd == null) {
			rnd = new Random();
		}
		final Vector result = new Vector(dim);
		for (int i = 0; i < dim; i++) {
			result.set(i, rnd.nextGaussian());
		}
		return result;
	}

	/**
	 * Creates a new dim-dimensional vector that is initialized with independent
	 * standard normal values (expectation of zero, variance of one).
	 * 
	 * @param dim
	 *            the dimension of the newly created vector; must be strictly
	 *            positive
	 * 
	 * @return a dim-dimensional vector that is initialized with independent
	 *         standard normal values
	 */
	public static Vector newGaussian(final int dim) {
		return newGaussian(dim, null);
	}

	/**
	 * Creates a "deep" copy of this vector. If enlargement is greater than
	 * zero, an appropriate number of zero entries is appended to the result.
	 * 
	 * @param enlargement
	 *            how many zeros are to be appended to the returned copy of this
	 *            instance; must not be negative
	 * 
	 * @return a "deep" copy of this instance to which enlargement zeros have
	 *         been appended
	 */
	public Vector copyEnlarged(final int enlargement) {
		if (enlargement < 0) {
			throw new IllegalArgumentException(
					"negative enlargment is not possible");
		}
		final Vector result = new Vector(this.data.length + enlargement);
		System.arraycopy(this.data, 0, result.data, 0, this.data.length);
		return result;
	}

	/**
	 * Returns a "deep" copy of this vector.
	 * 
	 * @return a "deep" copy of this vector
	 */
	public Vector copy() {
		return copyEnlarged(0);
	}

	/**
	 * Returns an immutable view on this vector.
	 * 
	 * @return a "deep" copy of this vector
	 */
	public Vector newImmutableView() {
		return new Vector(this.data, true);
	}

	// -------------------- WRITE ACCESS --------------------

	private void checkImmutable() {
		if (this.immutable) {
			throw new UnsupportedOperationException(
					"immutable Vector cannot be changed");
		}
	}

	public void copy(final Vector other) {
		this.checkImmutable();
		System.arraycopy(other.data, 0, this.data, 0, Math.min(this.size(),
				other.size()));
	}

	public void fill(final double value) {
		this.checkImmutable();
		Arrays.fill(this.data, value);
	}

	/**
	 * Clears this vector by setting all of its elements to zero.
	 */
	public void clear() {
		this.checkImmutable();
		this.fill(0.0);
	}

	/**
	 * Writes value at position pos of this vector.
	 * 
	 * @param pos
	 *            the position at which value is to be written
	 * 
	 * @param value
	 *            the value that is to be written
	 */
	public void set(final int pos, final double value) {
		this.checkImmutable();
		this.data[pos] = value;
	}

	/**
	 * Adds value to position pos of this vector.
	 * 
	 * @param pos
	 *            the position at which value is to be added
	 * 
	 * @param value
	 *            that value that is to be added
	 */
	public void add(final int pos, final double value) {
		this.checkImmutable();
		this.data[pos] += value;
	}

	/**
	 * Adds weight * other to this vector
	 * 
	 * @param other
	 *            the vector which to add, must not be null and of the same
	 *            dimension as this instance
	 * 
	 * @param weight
	 *            the value by which to multiply the values of other before
	 *            adding them to this vector
	 */
	public void add(final Vector other, final double weight) {
		this.checkImmutable();
		if (this.size() != other.size()) {
			throw new IllegalArgumentException(
					"vectors must be of same dimensions");
		}
		for (int i = 0; i < this.size(); i++)
			this.add(i, weight * other.get(i));
	}

	/**
	 * Multiplies the entry at position pos by value.
	 * 
	 * @param pos
	 *            the position at which to multiply
	 * 
	 * @param value
	 *            the value with which to multiply
	 */
	public void mult(final int pos, final double value) {
		this.checkImmutable();
		this.data[pos] *= value;
	}

	/**
	 * Multiplies every entry of this vector by value.
	 * 
	 * @param value
	 *            the value with which to multiply this vector
	 */
	public void mult(final double value) {
		this.checkImmutable();
		for (int i = 0; i < size(); i++) {
			mult(i, value);
		}
	}

	/**
	 * Normalizes this vector to length one.
	 */
	public void normalize() {
		this.checkImmutable();
		this.mult(1.0 / this.euclNorm());
	}

	/**
	 * Rounds the entries of this vector to decimals positions after the comma.
	 * If decimals is negative, the rounding is carried over to positions before
	 * the comma.
	 * 
	 * @param decimals
	 *            positions after the comma to which to round
	 */
	public void round(final int decimals) {
		this.checkImmutable();
		final double scale = Math.pow(10, decimals);
		for (int i = 0; i < this.size(); i++) {
			this.data[i] = Math.round(this.data[i] * scale) / scale;
		}
	}

	public void enforceBounds(final double lower, final double upper) {
		this.checkImmutable();
		for (int i = 0; i < this.size(); i++) {
			this.data[i] = Math.max(lower, Math.min(upper, this.data[i]));
		}
	}

	// -------------------- READ ACCESS --------------------

	public boolean isImmutable() {
		return this.immutable;
	}

	/**
	 * Returns the number of elements in this vector.
	 * 
	 * @return the number of elements in this vector
	 */
	public int size() {
		return this.data.length;
	}

	/**
	 * Returns the entry at position pos of this vector.
	 * 
	 * @param pos
	 *            the position of the desired entry
	 * 
	 * @return the entry at position pos of this vector
	 */
	public double get(final int pos) {
		return this.data[pos];
	}

	public double min() {
		double result = Double.POSITIVE_INFINITY;
		for (double x : this.data) {
			result = Math.min(result, x);
		}
		return result;
	}

	public double max() {
		double result = Double.NEGATIVE_INFINITY;
		for (double x : this.data) {
			result = Math.max(result, x);
		}
		return result;
	}

	/**
	 * Returns the inner product of this vector and other.
	 * 
	 * @param other
	 *            the vector with which to multiply, must not be null and of the
	 *            same dimension as this instance
	 * 
	 * @return the inner product of this vector and other
	 */
	public double innerProd(final Vector other) {
		if (this.size() != other.size()) {
			throw new IllegalArgumentException(
					"vectors must be of same dimensions");
		}
		double result = 0;
		for (int i = 0; i < this.size(); i++) {
			result += this.get(i) * other.get(i);
		}
		return result;
	}

	/**
	 * Returns the Euclidean norm of this vector.
	 * 
	 * @return the Euclidean norm of this vector
	 */
	public double euclNorm() {
		return Math.sqrt(this.innerProd(this));
	}

	public double sum() {
		double result = 0.0;
		for (double a : this.data) {
			result += a;
		}
		return result;
	}

	/**
	 * Returns the sum of the absolute values of all entries of this vector
	 * 
	 * @return the sum of the absolute values of all entries of this vector
	 */
	public double absValueSum() {
		double result = 0;
		for (int i = 0; i < this.data.length; i++) {
			result += Math.abs(this.data[i]);
		}
		return result;
	}
	
	public boolean isAllZeros() {
		for (int i = 0; i < this.data.length; i++) {
			if (this.data[i] != 0.0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns a textual representation of this vector.
	 * 
	 * @return a textual representation of this vector
	 */
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("[ ");
		for (int i = 0; i < size(); i++)
			result.append(get(i) + " ");
		result.append("]");
		return result.toString();
	}
}
