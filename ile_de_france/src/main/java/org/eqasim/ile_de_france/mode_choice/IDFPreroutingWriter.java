package org.eqasim.ile_de_france.mode_choice;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.matsim.core.utils.io.IOUtils;

public class IDFPreroutingWriter {
	private final File outputPath;

	private int cases = 0;
	private int switched = 0;

	private double switchedGain = 0.0;
	private double nonSwitchedGain = 0.0;

	public IDFPreroutingWriter(File outputPath) {
		this.outputPath = outputPath;
	}

	public void add(double gain, boolean switched) {
		if (switched) {
			this.switched++;
			this.switchedGain += gain;
		} else {
			this.nonSwitchedGain += gain;
		}

		this.cases++;
	}

	public void update(int iteration) {
		boolean writeHeader = !outputPath.exists();

		try {
			BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outputPath.toString());

			if (writeHeader) {
				writer.write(String.join(";", Arrays.asList( //
						"iteration", "cases", "switched", "switch_rate", "mean_gain", "mean_switched_gain",
						"mean_non_switched_gain" //
				)) + "\n");
			}

			writer.write(String.join(";", Arrays.asList( //
					String.valueOf(iteration), //
					String.valueOf(cases), //
					String.valueOf(switched), //
					String.valueOf((double) switched / (double) cases), //
					String.valueOf((switchedGain + nonSwitchedGain) / cases), //
					String.valueOf(switchedGain / switched), //
					String.valueOf(nonSwitchedGain / (cases - switched)) //
			)) + "\n");

			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		cases = 0;
		switchedGain = 0.0;
		nonSwitchedGain = 0.0;
		switched = 0;
	}
}
