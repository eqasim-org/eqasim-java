package org.eqasim.core.simulation.termination;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.TerminationCriterion;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;

public class EqasimTerminationModule extends AbstractModule {
	private static final String TERMINATION_CSV_FILE = "eqasim_termination.csv";
	private static final String TERMINATION_HTML_FILE = "eqasim_termination.html";

	@Override
	public void install() {
		bind(TerminationCriterion.class).to(EqasimTerminationCriterion.class);
	}

	@Provides
	@Singleton
	EqasimTerminationCriterion provideEqasimTerminationCriterion(ControlerConfigGroup controllerConfig,
			Map<String, TerminationIndicatorSupplier> indicators, Map<String, TerminationCriterionCalculator> criteria,
			TerminationWriter writer) {
		EqasimTerminationConfigGroup terminationConfig = EqasimTerminationConfigGroup.getOrCreate(getConfig());

		int firstIteration = controllerConfig.getFirstIteration();
		int lastIteration = controllerConfig.getLastIteration();

		if (terminationConfig.getHistoryFile() != null) {
			URL historyURL = ConfigGroup.getInputFileURL(getConfig().getContext(), terminationConfig.getHistoryFile());
			new TerminationReader(indicators.keySet(), criteria.keySet()).read(historyURL);
		}

		return new EqasimTerminationCriterion(firstIteration, lastIteration, indicators, criteria, writer);
	}

	@Provides
	@Singleton
	TerminationWriter provideTerminationWriter(OutputDirectoryHierarchy outputHierarchy,
			Map<String, TerminationIndicatorSupplier> indicators,
			Map<String, TerminationCriterionCalculator> criteria) {
		List<String> indicatorNames = new LinkedList<>(indicators.keySet());
		Collections.sort(indicatorNames);

		List<String> criterionNames = new LinkedList<>(criteria.keySet());
		Collections.sort(criterionNames);

		String outputCsvPath = outputHierarchy.getOutputFilename(TERMINATION_CSV_FILE);
		String outputHtmlPath = outputHierarchy.getOutputFilename(TERMINATION_HTML_FILE);

		return new TerminationWriter(outputCsvPath, outputHtmlPath, indicatorNames, criterionNames);
	}

	static public LinkedBindingBuilder<TerminationIndicatorSupplier> bindTerminationIndicator(Binder binder,
			String indicator) {
		return MapBinder.newMapBinder(binder, String.class, TerminationIndicatorSupplier.class).addBinding(indicator);
	}

	static public LinkedBindingBuilder<TerminationCriterionCalculator> bindTerminationCriterion(Binder binder,
			String criterion) {
		return MapBinder.newMapBinder(binder, String.class, TerminationCriterionCalculator.class).addBinding(criterion);
	}
}
