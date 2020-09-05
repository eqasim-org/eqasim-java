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
package cadyts.interfaces.sumo;

import java.util.logging.Logger;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.utilities.misc.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class SumoCalibrator extends AnalyticalCalibrator<String> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	static final double DEFAULT_DEMAND_SCALE = 1.0;

	static final boolean DEFAULT_OVERRIDE_TRAVELTIMES = false;

	static final String DEFAULT_FMA_PREFIX = null;

	static final String DEFAULT_CLONE_POSTFIX = "-CLONE";

	// -------------------- MEMBERS --------------------

	private double demandScale = DEFAULT_DEMAND_SCALE;

	private boolean overrideTravelTimes = DEFAULT_OVERRIDE_TRAVELTIMES;

	private DynamicData<String> travelTimes;

	private String fmaPrefix = DEFAULT_FMA_PREFIX;

	private String clonePostfix = DEFAULT_CLONE_POSTFIX;

	// -------------------- CONSTRUCTION --------------------

	SumoCalibrator(final String logFile, final Long randomSeed,
			final int binSize_s) {
		super(logFile, randomSeed, binSize_s);
		Logger.getLogger(this.myName).info(
				"default demandScale is " + this.demandScale);
		Logger.getLogger(this.myName).info(
				"default overrideTravelTimes is " + this.overrideTravelTimes);
		Logger.getLogger(this.myName).info(
				"default fmaPrefix is " + this.fmaPrefix);
		Logger.getLogger(this.myName).info(
				"default clonePostfix is " + this.clonePostfix);
	}

	// -------------------- SETTERS AND GETTERS --------------------

	void setDemandScale(final double demandScale) {
		if (demandScale < 1) {
			throw new IllegalArgumentException(
					"demandScale must not be smaller than one");
		}
		this.demandScale = demandScale;
	}

	double getDemandScale() {
		return this.demandScale;
	}

	void setOverrideTravelTimes(final boolean overrideTravelTimes) {
		this.overrideTravelTimes = overrideTravelTimes;
		Logger.getLogger(this.myName).info(
				"set overrideTravelTimes to " + this.overrideTravelTimes);
	}

	boolean getOverrideTravelTimes() {
		return this.overrideTravelTimes;
	}

	void overrideTravelTimes(final DynamicData<String> travelTimes) {
		if (!this.overrideTravelTimes) {
			Logger.getLogger(this.myName).warning(
					"ignoring call to overrideTravelTimes(..)");
			return;
		}
		if (this.travelTimes == null) {
			this.travelTimes = travelTimes;
		} else {
			this.travelTimes.overrideWithNonZeros(travelTimes);
		}
		Logger.getLogger(this.myName).fine("overrode travelTimes");
	}

	DynamicData<String> getTravelTimes() {
		return this.travelTimes;
	}

	void setFmaPrefix(final String fmaPrefix) {
		this.fmaPrefix = fmaPrefix;
		Logger.getLogger(this.myName)
				.info("set fmaPrefix to " + this.fmaPrefix);
	}

	String getFmaPrefix() {
		return this.fmaPrefix;
	}

	void setClonePostfix(final String clonePostfix) {
		if (clonePostfix == null) {
			throw new IllegalArgumentException("clonePostfix is null");
		}
		this.clonePostfix = clonePostfix;
		Logger.getLogger(this.myName).info(
				"set clonePostfix to " + this.clonePostfix);
	}

	String getClonePostfix() {
		return this.clonePostfix;
	}
}
