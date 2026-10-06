package org.eqasim.switzerland.ch_cmdp.utils.link_stats;

import java.io.IOException;
import java.io.UncheckedIOException;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

/** Collects sparse hourly link-entry volumes during the final iteration only. */
public class DetailedLinkStatisticsModule extends AbstractModule {
    @Override
    public void install() {
        addControllerListenerBinding().to(Listener.class);
    }

    static final class Listener implements IterationStartsListener, IterationEndsListener {
        private final Scenario scenario;
        private DetailedLinkStatisticsHandler handler;
        private SBBTransitConfigGroup transitConfig;
        private int previousLinkEventsInterval;
        private final OutputDirectoryHierarchy outputDirectoryHierarchy;

        @Inject
        Listener(Scenario scenario, OutputDirectoryHierarchy outputDirectoryHierarchy) {
            this.scenario = scenario;
            this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        }

        @Override
        public void notifyIterationStarts(IterationStartsEvent event) {
            if (!event.isLastIteration()) {
                return;
            }
            // QSim is created after IterationStarts. Deterministic SBB buses need
            // link events too, even when event-file writing has been disabled.
            if (scenario.getConfig().getModules().get(SBBTransitConfigGroup.GROUP_NAME)
                    instanceof SBBTransitConfigGroup config) {
                transitConfig = config;
                previousLinkEventsInterval = config.getCreateLinkEventsInterval();
                config.setCreateLinkEventsInterval(1);
            }
            handler = new DetailedLinkStatisticsHandler(scenario);
            event.getServices().getEvents().addHandler(handler);
        }

        @Override
        public void notifyIterationEnds(IterationEndsEvent event) {
            if (handler == null) {
                return;
            }
            event.getServices().getEvents().removeHandler(handler);
            String filename = outputDirectoryHierarchy.getOutputFilename("output_detailed_link_statistics.csv.gz");
            try {
                handler.write(filename);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot write detailed link statistics: " + filename, e);
            } finally {
                handler.reset(event.getIteration());
                handler = null;
                if (transitConfig != null) {
                    transitConfig.setCreateLinkEventsInterval(previousLinkEventsInterval);
                    transitConfig = null;
                }
            }
        }
    }
}
