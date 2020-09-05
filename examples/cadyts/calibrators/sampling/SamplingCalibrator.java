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

import cadyts.calibrators.Calibrator;

/**
 * 
 * A version of the Calibrator class for sampling-based plan selection. Does not
 * assume the true plan selection probabilities to be known.
 * 
 * @param L
 *            the network link type
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SamplingCalibrator<L> extends Calibrator<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_MAX_DRAWS = 20;

	// -------------------- MEMBERS --------------------

	private final Map<Object, ChoiceSampler<L>> samplers = new HashMap<Object, ChoiceSampler<L>>();

	private ChoiceSamplerFactory<L> choiceSamplerFactory;

	private int maxDraws = DEFAULT_MAX_DRAWS;

	// -------------------- CONSTRUCTION --------------------

	public SamplingCalibrator(final String logFile, final Long randomSeed,
			final int timeBinSize_s) {

		super(logFile, randomSeed, timeBinSize_s);
		this.choiceSamplerFactory = new RecursiveSamplerFactory<L>(this);

		Logger.getLogger(this.getClass().getName()).info(
				"default maxDraws is " + this.maxDraws);
		Logger.getLogger(this.getClass().getName()).info(
				"default choiceSamplerFactory is "
						+ RecursiveSamplerFactory.class.getSimpleName());
	}

	// -------------------- SIMPLE FUNCTIONALITY --------------------

	/**
	 * @param maxDraws
	 *            the maximum number of draws that is requested from a
	 *            replanning agent until a plan is accepted. Must be at least 3.
	 */
	public void setMaxDraws(final int maxDraws) {
		if (maxDraws < 2) {
			throw new IllegalArgumentException(
					"maximum number of draws must be at least 2");
		}
		this.maxDraws = maxDraws;
		Logger.getLogger(this.getClass().getName()).info(
				"set maxDraws to " + this.maxDraws);
	}

	public int getMaxDraws() {
		return this.maxDraws;
	}

	public void setChoiceSamplerFactory(
			final ChoiceSamplerFactory<L> choiceSamplerFactory) {
		if (choiceSamplerFactory == null) {
			throw new IllegalArgumentException(
					"choiceSamplerFactory must not be null");
		}
		this.choiceSamplerFactory = choiceSamplerFactory;
		Logger.getLogger(this.getClass().getName()).info(
				"set choiceSamplerFactory to "
						+ this.choiceSamplerFactory.getClass().getSimpleName());
	}

	public ChoiceSamplerFactory<L> getChoiceSamplerFactory() {
		return this.choiceSamplerFactory;
	}

	// -------------------- CALIBRATION LOGIC INTERFACE --------------------

	/**
	 * @param agent
	 *            the agent for which replanning is to be conducted
	 */
	public ChoiceSampler<L> getSampler(final Object agent) {

		// CHECK

		if (agent == null)
			throw new IllegalArgumentException(
					"reference agent must not be null;");

		// CONTINUE

		ChoiceSampler<L> sampler = this.samplers.get(agent);
		if (sampler == null) {
			sampler = this.getChoiceSamplerFactory().newSampler();
			this.samplers.put(agent, sampler);
		}
		if (this.getIteration() < this.getPreparatoryIterations()) {
			sampler.enforceNextAccept();
		}
		return sampler;
	}
}
