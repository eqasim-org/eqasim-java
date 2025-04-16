package org.eqasim.core.simulation.policies.impl.mobility_coins;

import java.io.File;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.policies.impl.mobility_coins.logic.MobilityCoinsCalculator;
import org.eqasim.core.simulation.policies.impl.mobility_coins.logic.MobilityCoinsMarket;
import org.eqasim.core.simulation.policies.impl.mobility_coins.logic.MobilityCoinsParameters;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class MobilityCoinsPolicyExtension extends AbstractEqasimExtension {
	private final CommandLine cmd;

	public MobilityCoinsPolicyExtension(CommandLine cmd) {
		this.cmd = cmd;
	}

	@Override
	protected void installEqasimExtension() {
		addControlerListenerBinding().to(MobilityCoinsMarket.class);
	}

	@Provides
	@Singleton
	MobilityCoinsParameters provideMobilityCoinsParameters() {
		MobilityCoinsParameters parameters = new MobilityCoinsParameters();
		ParameterDefinition.applyCommandLine("moco", cmd, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	MobilityCoinsPolicyFactory provideMobilityCoinsPolicyFactory(ModeParameters modeParameters,
			MobilityCoinsParameters parameters,
			MobilityCoinsCalculator calculator, MobilityCoinsMarket market) {
		return new MobilityCoinsPolicyFactory(getConfig(), modeParameters, parameters, calculator, market);
	}

	@Provides
	@Singleton
	MobilityCoinsCalculator provideMobilityCoinsCalculator(MobilityCoinsParameters parameters) {
		return new MobilityCoinsCalculator(parameters);
	}

	@Provides
	@Singleton
	MobilityCoinsMarket provideMobilityCoinsMarket(MobilityCoinsParameters parameters,
			MobilityCoinsCalculator calculator, Population population, MobilityCoinsWriter writer) {
		return new MobilityCoinsMarket(parameters, calculator, population, writer);
	}

	static public final String OUTPUT_PATH = "mobility_coins.csv";

	@Provides
	@Singleton
	MobilityCoinsWriter provideMobilityCoinsWriter(OutputDirectoryHierarchy outputDirectoryHierarchy) {
		File outputPath = new File(outputDirectoryHierarchy.getOutputFilename(OUTPUT_PATH));
		return new MobilityCoinsWriter(outputPath);
	}
}
