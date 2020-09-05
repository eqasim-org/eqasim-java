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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import cadyts.calibrators.analytical.AnalyticalCalibrator;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
class DraculaCalibrator extends AnalyticalCalibrator<DraculaLink> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	static final int DEFAULT_WARMUP_S = 0;

	static final double DEFAULT_BETA_TT_S = 0.0;

	static final int DEFAULT_DEMAND_PERIODS = 1;

	static final String DEFAULT_OD_PREFIX = null;

	static final double DEFAULT_DEMAND_SCALE = 1.0;

	static final String DEFAULT_FLOW_PREFIX = null;

	static final String DEFAULT_TRAVEL_TIME_PREFIX = null;

	// -------------------- MEMBERS --------------------

	private int warmUp_s = DEFAULT_WARMUP_S;

	private double betaTT_s = DEFAULT_BETA_TT_S;

	private int demandPeriods = DEFAULT_DEMAND_PERIODS;

	private String odPrefix = DEFAULT_OD_PREFIX;

	private double demandScale = DEFAULT_DEMAND_SCALE;

	private String flowPrefix = DEFAULT_FLOW_PREFIX;

	private String travelTimePrefix = DEFAULT_TRAVEL_TIME_PREFIX;

	private DraculaRoutes routes = null;

	private DraculaTravelTimes travelTimes = null;

	private final Map<DraculaLink, Double> minTravelTime_s = new LinkedHashMap<DraculaLink, Double>();

	// -------------------- CONSTRUCTION --------------------

	DraculaCalibrator(final String logFile, final Long randomSeed,
			final int timeBinSize_s) {

		super(logFile, randomSeed, timeBinSize_s);

		Logger.getLogger(this.getClass().getName()).info(
				"default warmup_s is " + this.warmUp_s);
		Logger.getLogger(this.getClass().getName()).info(
				"default betaTT_s is " + this.betaTT_s);
		Logger.getLogger(this.getClass().getName()).info(
				"default demandPeriods is " + this.demandPeriods);
		Logger.getLogger(this.getClass().getName()).info(
				"default odPrefix is " + this.odPrefix);
		Logger.getLogger(this.getClass().getName()).info(
				"default demandScale is " + this.demandScale);
		Logger.getLogger(this.getClass().getName()).info(
				"default flowPrefix is " + this.flowPrefix);
		Logger.getLogger(this.getClass().getName()).info(
				"default travelTimePrefix is " + this.travelTimePrefix);
	}

	// -------------------- SETTERS AND GETTERS --------------------

	void setWarmUp_s(final int warmUp_s) {
		if (warmUp_s < 0) {
			throw new IllegalArgumentException("warmup_s " + warmUp_s
					+ " is negative");
		}
		this.warmUp_s = warmUp_s;
		Logger.getLogger(this.getClass().getName()).info(
				"set warmUp_s to " + this.warmUp_s);
	}

	int getWarmUp_s() {
		return this.warmUp_s;
	}

	void setBetaTT_s(final double betaTT_s) {
		this.betaTT_s = betaTT_s;
		Logger.getLogger(this.getClass().getName()).info(
				"set betaTT_s to " + this.betaTT_s);
	}

	double getBetaTT_s() {
		return this.betaTT_s;
	}

	void setDemandPeriods(final int demandPeriods) {
		if (demandPeriods < 1) {
			throw new IllegalArgumentException("demandPeriods " + demandPeriods
					+ " is not strictly positive");
		}
		this.demandPeriods = demandPeriods;
		Logger.getLogger(this.getClass().getName()).info(
				"set demandPeriods to " + this.demandPeriods);
	}

	int getDemandPeriods() {
		return this.demandPeriods;
	}

	void setOdPrefix(final String odPrefix) {
		this.odPrefix = odPrefix;
		Logger.getLogger(this.getClass().getName()).info(
				"set odPrefix to " + this.odPrefix);
	}

	String getOdPrefix() {
		return this.odPrefix;
	}

	void setDemandScale(final double demandScale) {
		if (demandScale < 1.0) {
			throw new IllegalArgumentException(
					"demandScale must not be smaller than one");
		}
		this.demandScale = demandScale;
		Logger.getLogger(this.getClass().getName()).info(
				"set demandScale to " + this.demandScale);
	}

	double getDemandScale() {
		return this.demandScale;
	}

	void setRoutes(final DraculaRoutes routes) {
		if (routes == null) {
			throw new IllegalArgumentException("routes are null");
		}
		if (this.routes != null) {
			throw new UnsupportedOperationException(
					"routes have already been set");
		}
		this.routes = routes;
		Logger.getLogger(this.getClass().getName()).info("set routes");
	}

	DraculaRoutes getRoutes() {
		return this.routes;
	}

	void setTravelTimes(final DraculaTravelTimes travelTimes) {
		if (travelTimes == null) {
			throw new IllegalArgumentException("travel times are null");
		}
		this.travelTimes = travelTimes;
		Logger.getLogger(this.getClass().getName()).info("set travelTimes");
	}

	DraculaTravelTimes getTravelTimes() {
		return this.travelTimes;
	}

	void setFlowPrefix(final String flowPrefix) {
		this.flowPrefix = flowPrefix;
		Logger.getLogger(this.getClass().getName()).info(
				"set flowPrefix to " + this.flowPrefix);
	}

	String getFlowPrefix() {
		return this.flowPrefix;
	}

	void setTravelTimePrefix(final String travelTimePrefix) {
		this.travelTimePrefix = travelTimePrefix;
		Logger.getLogger(this.getClass().getName()).info(
				"set travelTimePrefix to " + this.travelTimePrefix);
	}

	String getTravelTimePrefix() {
		return this.travelTimePrefix;
	}

	Map<DraculaLink, Double> getMinTravelTimes() {
		return this.minTravelTime_s;
	}

}
