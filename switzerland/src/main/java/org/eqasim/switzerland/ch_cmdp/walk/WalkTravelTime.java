package org.eqasim.switzerland.ch_cmdp.walk;

/** Implementations are shared by routing workers and must be thread-safe.
 * Return walking time in seconds for whole walk trips and network-mode access/egress.
 * Public transport retains its own internal access/egress and transfer calculations.
 */
@FunctionalInterface
public interface WalkTravelTime {
    double calculate(WalkTravelTimeContext context);
}
