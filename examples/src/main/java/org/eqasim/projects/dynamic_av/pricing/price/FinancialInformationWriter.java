package org.eqasim.projects.dynamic_av.pricing.price;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

public class FinancialInformationWriter implements IterationEndsListener, ShutdownListener {
	private final File outputPath;
	private final PriceCalculator calculator;

	private BufferedWriter writer = null;

	public FinancialInformationWriter(File outputPath, PriceCalculator listener) {
		this.outputPath = outputPath;
		this.calculator = listener;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			if (writer == null) {
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));
				writer.write(String.join(";", new String[] { //
						"iteration", //

						"activeDistanceFare_MU_km", //

						"distanceFare_MU_km", //
						"baseFare_MU_km", //

						"costPerPassengerKm_CHF", //

						"fleetCost_MU", //
						"profit_MU" //
				}) + "\n");
			}

			FinancialInformation information = calculator.getInformation();

			writer.write(String.join(";", new String[] { //
					String.valueOf(event.getIteration()), //
					String.valueOf(calculator.getInterpolatedPricePerKm_CHF()), //
					String.valueOf(information.pricePerPassengerKm_CHF), //
					String.valueOf(information.pricePerTrip_CHF), //
					String.valueOf(information.costPerPassengerKm_CHF), //
					String.valueOf(information.fleetCost_CHF), //
					String.valueOf(information.profit_CHF) //
			}) + "\n");

			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
