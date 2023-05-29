package org.eqasim.ile_de_france.routing;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.matsim.core.config.CommandLine;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;

public class IDFRaptorModule extends AbstractModule {
	private final CommandLine commandLine;

	public IDFRaptorModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	public void install() {
	}

	@Provides
	@Singleton
	public IDFRaptorParameters provideEqasimRaptorParameters() {
		IDFRaptorParameters parameters = new IDFRaptorParameters();

		ParameterDefinition.applyCommandLine("raptor", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public RaptorStaticConfig provideRaptorStaticConfig(TransitSchedule schedule, IDFRaptorParameters parameters) {
		return IDFRaptorUtils.createRaptorStaticConfig(getConfig(), schedule, parameters);
	}

	@Provides
	@Singleton
	public RaptorParameters provideRaptorParameters(TransitSchedule schedule, IDFRaptorParameters parameters) {
		return IDFRaptorUtils.createRaptorParameters(getConfig(), schedule, parameters);
	}

	@Provides
	@Singleton
	public RaptorParametersForPerson provideRaptorParametersForPerson(RaptorParameters parameters) {
		return person -> parameters;
	}
}
