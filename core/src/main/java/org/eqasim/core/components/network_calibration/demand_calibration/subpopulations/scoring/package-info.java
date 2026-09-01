/**
 * Unified counted-route scoring and incrementally updated traffic state.
 * {@code TrafficScore} owns all score formulas, {@code RouteImpact} owns route
 * extraction, and {@code TrafficScoringTracker} owns only mutable traffic state.
 */
package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring;
