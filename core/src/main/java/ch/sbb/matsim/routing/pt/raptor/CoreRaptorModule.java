package ch.sbb.matsim.routing.pt.raptor;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.matsim.core.config.CommandLine;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class CoreRaptorModule extends AbstractModule {
	private final CommandLine commandLine;

	public CoreRaptorModule(CommandLine commandLine) {
		this.commandLine = commandLine;
	}

	@Override
	public void install() {
	}

	@Provides
	@Singleton
	public CoreRaptorParameters provideEqasimRaptorParameters() {
		CoreRaptorParameters parameters = new CoreRaptorParameters();

		ParameterDefinition.applyCommandLine("raptor", commandLine, parameters);
		return parameters;
	}

	@Provides
	@Singleton
	public RaptorStaticConfig provideRaptorStaticConfig(TransitSchedule schedule, CoreRaptorParameters parameters) {
		return CoreRaptorUtils.createRaptorStaticConfig(getConfig(), schedule, parameters);
	}

	@Provides
	@Singleton
	public RaptorParameters provideRaptorParameters(TransitSchedule schedule, CoreRaptorParameters parameters) {
		return CoreRaptorUtils.createRaptorParameters(getConfig(), schedule, parameters);
	}

	@Provides
	@Singleton
	public RaptorParametersForPerson provideRaptorParametersForPerson(RaptorParameters parameters) {
		return person -> parameters;
	}
}