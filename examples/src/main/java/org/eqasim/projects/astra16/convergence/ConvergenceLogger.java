package org.eqasim.projects.astra16.convergence;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class ConvergenceLogger implements IterationEndsListener {
	private final ConvergenceManager manager;
	private final Logger logger = Logger.getLogger(ConvergenceManager.class);

	public ConvergenceLogger(ConvergenceManager manager) {
		this.manager = manager;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int numberOfCriteria = 0;
		int numberOfFulfilledCriteria = 0;

		for (Map.Entry<String, ConvergenceCriterion> entry : manager.getCriteria().entrySet()) {
			ConvergenceCriterion criterion = entry.getValue();
			String name = entry.getKey();

			if (criterion.getValues().size() > 0) {

				double metric = criterion.getValues().get(criterion.getValues().size() - 1);
				double threshold = criterion.getThreshold();
				boolean isConverged = criterion.isConverged();

				logger.info(String.format("[Convergence] %s: %e <= %e ? %s", name, metric, threshold,
						isConverged ? "yes" : "no"));

				if (isConverged) {
					numberOfFulfilledCriteria += 1;
				}
			}

			numberOfCriteria += 1;
		}

		logger.info(String.format("[Convergence] GENERAL: %d/%d criteria fulfilled", numberOfFulfilledCriteria,
				numberOfCriteria));
	}
}
