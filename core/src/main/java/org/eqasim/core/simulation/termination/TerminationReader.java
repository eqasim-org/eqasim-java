package org.eqasim.core.simulation.termination;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

import com.google.common.collect.ImmutableMap;

public class TerminationReader {
	private final Collection<String> indicators;
	private final Collection<String> criteria;

	public TerminationReader(Collection<String> indicators, Collection<String> criteria) {
		this.indicators = indicators;
		this.criteria = criteria;
	}

	public List<TerminationData> read(URL inputURL) {
		List<TerminationData> data = new LinkedList<>();

		try {
			BufferedReader reader = IOUtils.getBufferedReader(inputURL);

			String rawLine = null;
			List<String> header = null;

			while ((rawLine = reader.readLine()) != null) {
				List<String> row = Arrays.asList(rawLine.split(";"));

				if (header == null) {
					header = row;
				} else {
					int iteration = Integer.parseInt(row.get(header.indexOf("iteration")));

					ImmutableMap.Builder<String, Double> criterionValues = ImmutableMap.builder();
					ImmutableMap.Builder<String, Double> indicatorValues = ImmutableMap.builder();

					for (String criterion : criteria) {
						int index = header.indexOf("criterion:" + criterion);

						if (index >= 0) {
							criterionValues.put(criterion, Double.parseDouble(row.get(index)));
						} else {
							criterionValues.put(criterion, Double.NaN);
						}
					}

					for (String indicator : indicators) {
						int index = header.indexOf("indicator:" + indicator);

						if (index >= 0) {
							indicatorValues.put(indicator, Double.parseDouble(row.get(index)));
						} else {
							indicatorValues.put(indicator, Double.NaN);
						}
					}

					data.add(new TerminationData(iteration, indicatorValues.build(), criterionValues.build()));
				}
			}

			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return data;
	}
}
