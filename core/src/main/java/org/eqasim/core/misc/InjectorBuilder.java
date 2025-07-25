package org.eqasim.core.misc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.analysis.ScoreStatsControlerListener;
import org.matsim.analysis.TravelDistanceStats;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;

public class InjectorBuilder {
	private final Logger logger = LogManager.getLogger(InjectorBuilder.class);

	private final Controler controller;

	public InjectorBuilder(Scenario scenario, EqasimConfigurator configurator) {
		this.controller = new Controler(scenario);
		configurator.configureController(controller);
	}

	public InjectorBuilder addOverridingModule(AbstractModule module) {
		controller.addOverridingModule(module);
		return this;
	}

	public InjectorBuilder addOverridingModules(List<AbstractModule> modules) {
		for (AbstractModule module : modules) {
			controller.addOverridingModule(module);
		}

		return this;
	}

	public com.google.inject.Injector build() {
		return build(true);
	}

	/**
	 * @param cleanOutputHierarchy If true, overrides the MATSim output directory to
	 *                             a temporary one and deletes it automatically when
	 *                             the process exits.
	 */
	public com.google.inject.Injector build(boolean cleanOutputHierarchy) {
		final Path temporaryPath;

		if (cleanOutputHierarchy) {
			try {
				temporaryPath = Files.createTempDirectory("eqasim_standalone");
				controller.getConfig().controller().setOutputDirectory(temporaryPath.toString());
				logger.info("Setting output directory to " + temporaryPath.toString());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			temporaryPath = null;
		}

		com.google.inject.Injector injector = controller.getInjector();

		if (cleanOutputHierarchy) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					injector.getInstance(TravelDistanceStats.class).close();
					injector.getInstance(ScoreStatsControlerListener.class)
							.notifyShutdown(new ShutdownEvent(null, false, 0, null));

					logger.info("Deleting output directory " + temporaryPath.toString());
					FileUtils.deleteDirectory(new File(temporaryPath.toString()));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}));
		}

		return injector;
	}
}