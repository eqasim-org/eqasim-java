package org.eqasim.automated_vehicles.mode_choice.cost;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

public class AvCostWriter implements Closeable, IterationEndsListener {
	private final File outputPath;
	private final AvCostListener listener;

	private BufferedWriter writer = null;

	public AvCostWriter(File outputPath, AvCostListener listener) {
		this.outputPath = outputPath;
		this.listener = listener;
	}

	@Override
	public void close() throws IOException {
		if (writer != null) {
			writer.close();
		}
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
}
