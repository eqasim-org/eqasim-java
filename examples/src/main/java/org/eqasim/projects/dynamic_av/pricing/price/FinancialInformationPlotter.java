package org.eqasim.projects.dynamic_av.pricing.price;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.charts.XYLineChart;

public class FinancialInformationPlotter implements IterationEndsListener {
	private final PriceCalculator calculator;
	private final OutputDirectoryHierarchy outputHierarchy;

	private Map<Integer, Double> activeFareHistory = new HashMap<>();
	private Map<Integer, Double> computedFareHistory = new HashMap<>();

	public FinancialInformationPlotter(PriceCalculator listener, OutputDirectoryHierarchy outputHierarchy) {
		this.calculator = listener;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (event.getIteration() == 10) {
			activeFareHistory.clear();
			computedFareHistory.clear();
		}

		FinancialInformation information = calculator.getInformation();

		activeFareHistory.put(event.getIteration(), calculator.getInterpolatedPricePerKm_CHF());
		computedFareHistory.put(event.getIteration(), information.pricePerPassengerKm_CHF);

		XYLineChart chart = new XYLineChart("Waiting time prediction", "Iteration", "Error");

		chart.addSeries("Active", activeFareHistory);
		chart.addSeries("Computed", computedFareHistory);

		chart.saveAsPng(outputHierarchy.getOutputFilename("distance_fare.png"), 800, 600);
	}
}
