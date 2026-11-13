package org.eqasim.switzerland.ch_cmdp.walk;

/** Retains MATSim's configured teleportation speed, distance factor and rounding at each call site. */
public final class TeleportationWalkTravelTime implements WalkTravelTime {
    @Override public double calculate(WalkTravelTimeContext context) {
        return context.defaultTravelTime();
    }
}
