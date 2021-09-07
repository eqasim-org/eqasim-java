package org.eqasim.core.simulation.convergence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

public class ConvergenceSignal {
	private final String name;
	private final List<Integer> iterations = new LinkedList<>();
	private final List<Double> values = new LinkedList<>();

	public ConvergenceSignal(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<Integer> getIterations() {
		return iterations;
	}

	public List<Double> getValues() {
		return values;
	}

	public void addValue(int iteration, double value) {
		this.values.add(value);
		this.iterations.add(iteration);
	}

	public void write(File outputFile) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputFile.toString());

			writer.write(String.join(";", new String[] { "name", "iteration", "value" }) + "\n");

			for (int i = 0; i < iterations.size(); i++) {
				writer.write(String.join(";",
						new String[] { name, String.valueOf(iterations.get(i)), String.valueOf(values.get(i)) })
						+ "\n");
			}

			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void read(File inputFile) {
		try {
			BufferedReader reader = IOUtils.getBufferedReader(inputFile.toString());

			String line = null;
			List<String> header = null;

			while ((line = reader.readLine()) != null) {
				List<String> row = Arrays.asList(line.split(";"));

				if (header == null) {
					header = row;
				} else {
					String name = row.get(header.indexOf("name"));
					int iteration = Integer.parseInt(row.get(header.indexOf("iteration")));
					double value = Double.parseDouble(row.get(header.indexOf("value")));

					if (!name.equals(this.name)) {
						throw new IllegalStateException("Reading wrong file? " + name + " vs " + this.name);
					}

					values.add(value);
					iterations.add(iteration);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
