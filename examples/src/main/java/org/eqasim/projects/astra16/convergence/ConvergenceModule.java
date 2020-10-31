package org.eqasim.projects.astra16.convergence;

import org.eqasim.projects.astra16.convergence.metrics.AbsoluteMeanDifference;
import org.eqasim.projects.astra16.convergence.metrics.AbsoluteMeanDistance;
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

		/*-if (false) {
			for (int horizon : Arrays.asList(25, 50, 100)) {
				manager.addCriterion("waitingTimeError_h" + horizon,
						new ConvergenceCriterion("waitingTimeError", new AbsoluteMeanDistance(horizon), 15.0));
				manager.addCriterion("travelTimeError_h" + horizon,
						new ConvergenceCriterion("travelTimeError", new AbsoluteMeanDistance(horizon), 60.0));
				manager.addCriterion("activePrice_h" + horizon,
						new ConvergenceCriterion("activePrice", new AbsoluteMeanDistance(horizon), 0.01));
				manager.addCriterion("amodRequests_h" + horizon,
						new ConvergenceCriterion("amodRequests", new RelativeMeanDistance(horizon), 0.01));
			}
		
			manager.addCriterion("activePriceChange",
					new ConvergenceCriterion("activePrice", new AbsoluteDifference(10), 0.01));
			manager.addCriterion("amodRequestsChange",
					new ConvergenceCriterion("amodRequests", new RelativeDifference(10), 0.01));
		}-*/

		int horizon = 50;
		int lag = 25;

		manager.addCriterion("waitingTimeError",
				new ConvergenceCriterion("waitingTimeError", new AbsoluteMeanDistance(horizon), 15.0));
		manager.addCriterion("travelTimeError",
				new ConvergenceCriterion("travelTimeError", new AbsoluteMeanDistance(horizon), 60.0));
		manager.addCriterion("activePrice",
				new ConvergenceCriterion("activePrice", new AbsoluteMeanDistance(horizon), 0.01));
		manager.addCriterion("amodRequests",
				new ConvergenceCriterion("amodRequests", new AbsoluteMeanDistance(horizon), 100));

		manager.addCriterion("waitingTimeErrorMean",
				new ConvergenceCriterion("waitingTimeError", new AbsoluteMeanDifference(horizon, lag), 1.0));
		manager.addCriterion("travelTimeErrorMean",
				new ConvergenceCriterion("travelTimeError", new AbsoluteMeanDifference(horizon, lag), 1.0));
		manager.addCriterion("activePriceMean",
				new ConvergenceCriterion("activePrice", new AbsoluteMeanDifference(horizon, lag), 0.01));
		manager.addCriterion("amodRequestsMean",
				new ConvergenceCriterion("amodRequests", new AbsoluteMeanDifference(horizon, lag), 100));

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
