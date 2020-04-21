package org.eqasim.examples.zurich_adpt.mode_choice;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.examples.zurich_adpt.mode_choice.constraints.AdPTConstraint;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.AdPTCostModel;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.CordonPricingCarCostModel;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.ZonalVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.mode_parameters.AdPTModeParameters;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.estimators.AdPTUtilityEstimator;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.predictors.AdPTPredictor;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.CordonChargingData;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zone;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.utils.io.IOUtils;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class AdPTModeChoiceModule extends AbstractEqasimExtension {
	private final CommandLine commandLine;

	static public final String ADPT_ESTIMATOR_NAME = "AdPTEstimator";
	static public final String ADPT_COST_MODEL_NAME = "AdPTCostModel";
	static public final String ADPT_CONSTRAINT_NAME = "AdptConstraint";

	public static final String CAR_COST_MODEL_NAME = "CordonPricingCarCostModel";

	public AdPTModeChoiceModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	protected void installEqasimExtension() {
		bindUtilityEstimator(ADPT_ESTIMATOR_NAME).to(AdPTUtilityEstimator.class);
		bindCostModel(ADPT_COST_MODEL_NAME).to(AdPTCostModel.class);
		bindCostModel(CAR_COST_MODEL_NAME).to(CordonPricingCarCostModel.class);
		bind(AdPTPredictor.class);
		bindTripConstraintFactory(ADPT_CONSTRAINT_NAME).to(AdPTConstraint.Factory.class);

	}

	@Provides
	@Singleton
	public CordonChargingData provideCordonChargingData()
			throws MalformedURLException, IOException, ConfigurationException {
		// read cordon
		Map<String, Zone> mapCordon = Zone.read(new File(this.commandLine.getOptionStrict("cordon-shapefile")));

		Map<Integer, Double> mapCordCharges = new HashMap<>();

		BufferedReader readerCharges = IOUtils.getBufferedReader(this.commandLine.getOptionStrict("cordon-charges"));

		readerCharges.readLine();
		String s = readerCharges.readLine();

		while (s != null) {
			String[] values = s.split(",");
			mapCordCharges.put(Integer.parseInt(values[0]), Double.parseDouble(values[1]));

			s = readerCharges.readLine();
		}

		return new CordonChargingData(mapCordCharges, mapCordon);
	}

	@Provides
	@Singleton
	public AdPTCostModel provideAdPTCostModel(ZonalVariables zonalVariables) {
		return new AdPTCostModel(zonalVariables);
	}

	@Provides
	@Singleton
	public AdPTModeParameters provideAdPTModeParameters() {
		AdPTModeParameters parameters = AdPTModeParameters.buildDefault();

		return parameters;
	}

}
