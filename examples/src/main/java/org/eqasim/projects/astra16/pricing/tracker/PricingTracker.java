package org.eqasim.projects.astra16.pricing.tracker;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eqasim.projects.astra16.pricing.business_model.BusinessModelData;
import org.eqasim.projects.astra16.pricing.business_model.BusinessModelListener;
import org.eqasim.projects.astra16.pricing.model.PriceInterpolator;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.charts.XYLineChart;

public class PricingTracker implements IterationEndsListener, BusinessModelListener {
	private final PriceInterpolator interpolator;
	private final OutputDirectoryHierarchy outputHierarchy;

	private final List<Double> activeValues = new LinkedList<>();
	private final List<Double> nominalValues = new LinkedList<>();

	public PricingTracker(PriceInterpolator interpolator, OutputDirectoryHierarchy outputHierarchy) {
		this.interpolator = interpolator;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		activeValues.add(interpolator.getPricePerKm_CHF());

		Map<Integer, Double> activeData = new HashMap<>();
		Map<Integer, Double> nominalData = new HashMap<>();

		for (int i = 0; i < event.getIteration(); i++) {
			double activeValue = activeValues.get(i);

			if (Double.isFinite(activeValue)) {
				activeData.put(i, activeValue);
			}

			double nominalValue = nominalValues.get(i);

			if (Double.isFinite(nominalValue)) {
				nominalData.put(i, nominalValue);
			}
		}

		XYLineChart chart = new XYLineChart("Distance fare [CHF/km]", "Iteration", "Distance fare [CHF/km]");

		chart.addSeries("Active", activeData);
		chart.addSeries("Computed", nominalData);

		chart.saveAsPng(outputHierarchy.getOutputFilename("distance_fare.png"), 800, 600);
	}

	@Override
	public void handleBusinessModel(BusinessModelData model) {
		nominalValues.add(model.pricePerPassengerKm_CHF);
	}
}
