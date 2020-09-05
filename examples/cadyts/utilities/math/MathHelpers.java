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

import java.util.Random;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MathHelpers {

	// -------------------- PRIVATE CONSTRUCTOR --------------------

	private MathHelpers() {
	}

	// -------------------- STATIC IMPLEMENTATION --------------------

	public static double overlap(final double start1, final double end1,
			final double start2, final double end2) {
		return Math.max(0, (Math.min(end1, end2) - Math.max(start1, start2)));
	}

	public static double round(final double x, final int digits) {
		final double fact = Math.pow(10.0, digits);
		return Math.round(x * fact) / fact;
	}

	public static int round(final double x) {
		return (int) round(x, 0);
	}

	public static int draw(final Vector probs, final Random rnd) {
		final double x = rnd.nextDouble();
		int result = -1;
		double pSum = 0;
		do {
			result++;
			pSum += probs.get(result);
		} while (pSum < x && result < probs.size() - 1);
		return result;
	}

	public static double logOfFactorial(final int x) {
		double result = 0;
		for (int y = 2; y <= x; y++) {
			result += Math.log(y);
		}
		return result;
	}

	public static double logOfMultinomialCoefficient(final int... values) {
		double result = 0;
		int sum = 0;
		for (int k : values) {
			result -= logOfFactorial(k);
			sum += k;
		}
		result += logOfFactorial(sum);
		return result;
	}

	public static double[] override(final double[] dest, final double[] source,
			final boolean overrideWithZeros) {
		if (source == null) {
			if (overrideWithZeros) {
				return null;
			} else {
				return dest;
			}
		} else { // source != null
			if (dest == null) {
				final double[] result = new double[source.length];
				System.arraycopy(source, 0, result, 0, source.length);
				return result;
			} else { // dest != null
				for (int i = 0; i < source.length; i++) {
					if (overrideWithZeros || source[i] != 0.0) {
						dest[i] = source[i];
					}
				}
				return dest;
			}
		}
	}
}
