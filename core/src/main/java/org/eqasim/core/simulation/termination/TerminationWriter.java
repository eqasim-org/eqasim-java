package org.eqasim.core.simulation.termination;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TerminationWriter {
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final String outputCsvPath;
	private final String outputHtmlPath;

	private final List<String> indicators;
	private final List<String> criteria;

	public TerminationWriter(String outputCsvPath, String outputHtmlPath, List<String> indicators,
			List<String> criteria) {
		this.outputCsvPath = outputCsvPath;
		this.outputHtmlPath = outputHtmlPath;
		this.indicators = indicators;
		this.criteria = criteria;
	}

	public void write(List<TerminationData> data) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(outputCsvPath);

			List<String> header = new LinkedList<>();
			header.add("iteration");

			for (String indicator : indicators) {
				header.add("indicator:" + indicator);
			}

			for (String criterion : criteria) {
				header.add("criterion:" + criterion);
			}

			writer.write(String.join(";", header) + "\n");

			for (TerminationData item : data) {
				List<String> row = new LinkedList<>();
				row.add(String.valueOf(item.iteration));

				for (String indicator : indicators) {
					row.add(String.valueOf(item.indicators.get(indicator)));
				}

				for (String criterion : criteria) {
					row.add(String.valueOf(item.criteria.get(criterion)));
				}

				writer.write(String.join(";", row) + "\n");
			}

			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(TerminationWriter.class.getResourceAsStream("termination.html")));

			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(new File(outputHtmlPath))));

			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.indexOf("const data = {};") >= 0) {
					String jsonData = objectMapper.writeValueAsString(data);
					writer.write("const data = ");
					writer.write(jsonData);
					writer.write(";\n");
				} else {
					writer.write(line + "\n");
				}
			}

			writer.close();
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
