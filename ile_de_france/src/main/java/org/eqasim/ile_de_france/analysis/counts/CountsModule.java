package org.eqasim.ile_de_france.analysis.counts;

import java.io.File;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CountsModule extends AbstractModule {
	private final Set<Id<Link>> linkIds;

	public CountsModule(CommandLine cmd) {
		try {
			this.linkIds = new CountsReader().readLinks(new File(cmd.getOptionStrict("counts-path")));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void install() {
		addControlerListenerBinding().to(CountsListener.class);
	}

	@Provides
	@Singleton
	public CountsListener provideCountsListener(EqasimConfigGroup eqasimConfigGroup, EventsManager eventsManager,
			OutputDirectoryHierarchy outputDirectoryHierarchy, Network network) {
		return new CountsListener(eqasimConfigGroup, eventsManager, outputDirectoryHierarchy, network, linkIds);
	}
}
