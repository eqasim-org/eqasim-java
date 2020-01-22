package org.eqasim.projects.dynamic_av.pricing;

import java.io.File;

import org.eqasim.automated_vehicles.components.EqasimAvConfigGroup;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.projects.dynamic_av.pricing.cost_calculator.CostCalculator;
import org.eqasim.projects.dynamic_av.pricing.cost_calculator.definitions.ScenarioDefinition;
import org.eqasim.projects.dynamic_av.pricing.price.FinancialInformationPlotter;
import org.eqasim.projects.dynamic_av.pricing.price.FinancialInformationWriter;
import org.eqasim.projects.dynamic_av.pricing.price.PriceCalculator;
import org.eqasim.projects.dynamic_av.pricing.price.ProjectAvCostModel;
import org.eqasim.projects.dynamic_av.pricing.price.ProjectCostParameters;
import org.matsim.core.config.CommandLine;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.ethz.matsim.av.analysis.FleetDistanceListener;
import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;

public class PricingModule extends AbstractEqasimExtension {
	static public final String PROJECT_AV_COST_MODEL_NAME = "ProjectAvCostModel";

	private final CommandLine commandLine;

	public PricingModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	public void installEqasimExtension() {
		addEventHandlerBinding().to(PriceCalculator.class);
		addControlerListenerBinding().to(PriceCalculator.class);

		bind(ProjectAvCostModel.class);
		bindCostModel(PROJECT_AV_COST_MODEL_NAME).to(ProjectAvCostModel.class);

		addControlerListenerBinding().to(FinancialInformationWriter.class);
		addControlerListenerBinding().to(FinancialInformationPlotter.class);
	}

	@Provides
	@Singleton
	public ScenarioDefinition provideScenarioDefinition() {
		return ScenarioDefinition.buildForProject();
	}

	@Provides
	@Singleton
	public CostCalculator provideCostCalculator(ScenarioDefinition scenario) {
		return new CostCalculator(scenario);
	}

	@Provides
	@Singleton
	public PriceCalculator providePriceCalculator(ProjectCostParameters costParameters, CostCalculator calculator,
			FleetDistanceListener fleetDistanceListener, AVConfigGroup config, EqasimConfigGroup eqasimConfig) {
		int numberOfVehicles = config.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID).getGeneratorConfig()
				.getNumberOfVehicles();

		return new PriceCalculator(costParameters, fleetDistanceListener, numberOfVehicles, calculator,
				eqasimConfig.getSampleSize());
	}

	@Provides
	@Singleton
	public FinancialInformationWriter provideFinancialInformationWriter(OutputDirectoryHierarchy outputDirectory,
			PriceCalculator calculator) {
		File outputPath = new File(outputDirectory.getOutputFilename("project_av_prices.csv"));
		return new FinancialInformationWriter(outputPath, calculator);
	}

	@Provides
	@Singleton
	public FinancialInformationPlotter provideFinancialInformationPlotter(OutputDirectoryHierarchy outputDirectory,
			PriceCalculator calculator) {
		return new FinancialInformationPlotter(calculator, outputDirectory);
	}

	@Provides
	@Singleton
	public ProjectCostParameters provideProjectCostParameters(EqasimAvConfigGroup config) {
		ProjectCostParameters parameters = ProjectCostParameters.buildDefault();
		ParameterDefinition.applyCommandLine("project-cost-parameter", commandLine, parameters);
		return parameters;
	}
}
