package org.eqasim.projects.astra16.pricing.tracker;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.projects.astra16.pricing.business_model.BusinessModelData;
import org.eqasim.projects.astra16.pricing.business_model.BusinessModelListener;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class BusinessModelTracker implements BusinessModelListener, IterationEndsListener {
	private final List<BusinessModelData> history = new LinkedList<>();

	private final OutputDirectoryHierarchy outputHierarchy;

	public BusinessModelTracker(OutputDirectoryHierarchy outputHierarchy) {
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void handleBusinessModel(BusinessModelData model) {
		history.add(model);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		try {
			CsvMapper mapper = new CsvMapper();
			CsvSchema schema = mapper.schemaFor(BusinessModelData.class).withColumnSeparator(';').withHeader();

			// Write CSV
			File outputPath = new File(outputHierarchy.getOutputFilename("business_model.csv"));
			mapper.writer(schema).writeValue(outputPath, history);
			
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
