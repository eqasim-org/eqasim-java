package org.eqasim.projects.astra16.convergence;

import java.util.Arrays;

import org.eqasim.projects.astra16.convergence.metrics.AbsoluteDifference;
import org.eqasim.projects.astra16.convergence.metrics.AbsoluteMeanDistance;
import org.eqasim.projects.astra16.convergence.metrics.RelativeDifference;
import org.eqasim.projects.astra16.convergence.metrics.RelativeMeanDistance;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ConvergenceModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(ConvergenceWriter.class);
		addControlerListenerBinding().to(ConvergencePlotter.class);
		addControlerListenerBinding().to(ConvergenceLogger.class);
		addControlerListenerBinding().to(ManagedConvergenceCriterion.class);
		bind(TerminationCriterion.class).to(ManagedConvergenceCriterion.class);
	}

	@Provides
	@Singleton
	public ConvergenceManager provideConvergencemanager() {
		ConvergenceManager manager = new ConvergenceManager();

		for (int horizon : Arrays.asList(25, 50, 100)) {
			manager.addCriterion("waitingTimeError_h" + horizon,
					new ConvergenceCriterion("waitingTimeError", new AbsoluteMeanDistance(), 15.0, horizon));
			manager.addCriterion("travelTimeError_h" + horizon,
					new ConvergenceCriterion("travelTimeError", new AbsoluteMeanDistance(), 60.0, horizon));
			manager.addCriterion("activePrice_h" + horizon,
					new ConvergenceCriterion("activePrice", new AbsoluteMeanDistance(), 0.01, horizon));
			manager.addCriterion("amodRequests_h" + horizon,
					new ConvergenceCriterion("amodRequests", new RelativeMeanDistance(), 0.01, horizon));
		}

		manager.addCriterion("activePriceChange",
				new ConvergenceCriterion("activePrice", new AbsoluteDifference(), 0.01, 10));
		manager.addCriterion("amodRequestsChange",
				new ConvergenceCriterion("amodRequests", new RelativeDifference(), 0.01, 10));

		return manager;
	}

	@Provides
	@Singleton
	public ConvergenceWriter provideConvergenceWriter(ConvergenceManager manager,
			OutputDirectoryHierarchy outputHierarchy) {
		return new ConvergenceWriter(manager, outputHierarchy);
	}

	@Provides
	@Singleton
	public ConvergencePlotter provideConvergencePlotter(ConvergenceManager manager,
			OutputDirectoryHierarchy outputHierarchy) {
		return new ConvergencePlotter(manager, outputHierarchy);
	}

	@Provides
	@Singleton
	public ConvergenceLogger provideConvergenceLogger(ConvergenceManager manager) {
		return new ConvergenceLogger(manager);
	}

	@Provides
	@Singleton
	public ManagedConvergenceCriterion provideManagerConvergenceCriterion(ConvergenceManager manager) {
		return new ManagedConvergenceCriterion(manager);
	}
}
