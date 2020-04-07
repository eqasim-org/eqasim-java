package org.eqasim.projects.astra16.pricing.model;

import org.eqasim.projects.astra16.pricing.business_model.BusinessModelData;
import org.eqasim.projects.astra16.pricing.business_model.BusinessModelListener;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

public class PriceInterpolator implements BusinessModelListener, IterationStartsListener {
	private final int startIteration;
	private final double factor;

	private double pricePerTrip_CHF = 0.0;
	private double pricePerKm_CHF = 0.0;

	private int iteration = 0;

	public PriceInterpolator(double factor, double initialPricePerKm_CHF, int startIteration) {
		this.factor = factor;
		this.startIteration = startIteration;
		this.pricePerKm_CHF = initialPricePerKm_CHF;
	}

	public double getPricePerKm_CHF() {
		return pricePerKm_CHF;
	}

	public double getPricePerTrip_CHF() {
		return pricePerTrip_CHF;
	}

	@Override
	public void handleBusinessModel(BusinessModelData model) {
		pricePerTrip_CHF = model.pricePerTrip_CHF;

		if (Double.isFinite(model.pricePerPassengerKm_CHF) && iteration >= startIteration) {
			pricePerKm_CHF = (1.0 - factor) * pricePerKm_CHF + factor * model.pricePerPassengerKm_CHF;
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		iteration = event.getIteration();
	}
}
