package org.eqasim.ile_de_france.analysis.flow;

import java.io.IOException;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class FlowModule extends AbstractModule {
	private final String inputPath;

	public FlowModule(String inputPath) {
		this.inputPath = inputPath;
	}

	@Override
	public void install() {
		addControlerListenerBinding().to(FlowListener.class);
	}

	@Singleton
	@Provides
	FlowListener provideFlowListener(EqasimConfigGroup eqasimConfig, ControlerConfigGroup controllerConfig,
			OutputDirectoryHierarchy outputDirectoryHierarchy) {
		try {
			IdSet<Link> linkIds = new FlowReader().read(inputPath);
			return new FlowListener(eqasimConfig, controllerConfig, outputDirectoryHierarchy, linkIds);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
