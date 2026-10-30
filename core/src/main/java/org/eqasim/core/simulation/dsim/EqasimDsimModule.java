package org.eqasim.core.simulation.dsim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eqasim.core.components.traffic.CrossingPenalty;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.dsim.simulation.net.DefaultWait2Link;
import org.matsim.dsim.simulation.net.SimLink;
import org.matsim.dsim.simulation.net.SimNetwork;
import org.matsim.dsim.simulation.net.Wait2Link;

/** Single-JVM DSim integration; shared controller datasets and partition-local movement state. */
public final class EqasimDsimModule extends AbstractModule {
    private static final String COMPONENT = "EqasimDSim";

    /** Install last through Controler.addOverridingQSimModule, after DSim's own modules. */
    public static AbstractQSimModule engineModule() {
        return new AbstractQSimModule() {
            @Override protected void configureQSim() {
                bind(Wait2Link.class).to(DefaultWait2Link.class).in(Singleton.class);
                // Timetable passengers need no vehicle-boarding delegate. The native transit
                // factory's agent type is not registered by the population source in 2026.0.
                bind(org.matsim.core.mobsim.qsim.agents.AgentFactory.class)
                        .to(org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory.class).in(Singleton.class);
                bind(RoadDelays.class).in(Singleton.class);
                bind(ScheduledTransitEngine.class).in(Singleton.class);
                addQSimComponentBinding(COMPONENT).to(RoadDelays.class);
                if (getConfig().transit().isUseTransit()) {
                    addQSimComponentBinding(COMPONENT).to(ScheduledTransitEngine.class);
                }
            }
        };
    }

    @Override public void install() {
        binder().requestInjection(new Object() {
            @Inject void configure(QSimComponentsConfig components) {
                if (components.hasNamedComponent(TransitEngineModule.TRANSIT_ENGINE_NAME)) {
                    components.removeNamedComponent(TransitEngineModule.TRANSIT_ENGINE_NAME);
                }
                components.addNamedComponent(COMPONENT);
            }
        });
    }

    /** Uses the same stateful penalty instance as QSim, once on each traversed road link. */
    public static final class RoadDelays implements DistributedMobsimEngine {
        private final SimNetwork simulation;
        private final Network network;
        private final CrossingPenalty penalty;

        @Inject public RoadDelays(SimNetwork simulation, Network network, CrossingPenalty penalty) {
            this.simulation = simulation;
            this.network = network;
            this.penalty = penalty;
        }

        @Override public void beforeMobsim() {
            for (var simLink : simulation.getLinks().values()) {
                if (simLink instanceof SimLink.SplitOutLink) continue;
                var link = network.getLinks().get(simLink.getId());
                simLink.setLinkTravelTime((vehicle, now) -> link.getLength()
                        / Math.min(link.getFreespeed(now), vehicle.getMaximumVelocity())
                        + penalty.calculateCrossingPenalty(link, now, vehicle.getId()));
            }
        }
        @Override public void doSimStep(double now) { }
        @Override public void afterMobsim() { }
        @Override public void setInternalInterface(InternalInterface internalInterface) { }
    }
}
