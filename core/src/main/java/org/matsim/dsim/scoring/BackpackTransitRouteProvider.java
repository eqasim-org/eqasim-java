/* Adapted from MATSim 2026.0 BackpackTransitRouteProvider.java (GPL-2.0-or-later).
 * Upstream: https://github.com/matsim-org/matsim-libs/blob/2026.0/matsim/src/main/java/org/matsim/dsim/scoring/BackpackTransitRouteProvider.java
 * Adds experienced-route recording for Eqasim's timetable teleportation.
 */
package org.matsim.dsim.scoring;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

class BackpackTransitRouteProvider implements BackpackRouteProvider {
    private final Network network;
    private final TransitSchedule schedule;
    private final boolean teleported;

    @Inject BackpackTransitRouteProvider(Network network, TransitSchedule schedule, Config config) {
        this.network = network;
        this.schedule = schedule;
        teleported = !config.transit().isUsingTransitInMobsim();
    }

    @Override public BackpackRoute get() {
        return teleported ? new ScheduledTransitBackpackRoute() : new BackpackTransitRoute(network, schedule);
    }

    @Override public BackpackRoute get(Message message) {
        if (message instanceof ScheduledTransitBackpackRoute route) return route;
        if (message instanceof BackpackTransitRoute.Msg msg) return new BackpackTransitRoute(network, schedule, msg.parts());
        throw new IllegalArgumentException("Unsupported transit backpack message: " + message.getClass());
    }
}
