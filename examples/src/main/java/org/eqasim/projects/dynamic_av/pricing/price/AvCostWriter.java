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

public class AvCostWriter implements IterationEndsListener, ShutdownListener {
	private final File outputPath;
	private final PriceCalculator listener;

	private BufferedWriter writer = null;

	public AvCostWriter(File outputPath, PriceCalculator listener) {
		this.outputPath = outputPath;
		this.listener = listener;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			if (writer == null) {
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));
				writer.write(String.join(";", new String[] { //
						"iteration", //
						"observed_price_MU_km", //
						"active_price_MU_km", //
				}) + "\n");
			}

			writer.write(String.join(";", new String[] { //
					String.valueOf(event.getIteration()), //
					String.valueOf(listener.getObservedPrice_MU_km()), //
					String.valueOf(listener.getActivePrice_MU_km()) //
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
