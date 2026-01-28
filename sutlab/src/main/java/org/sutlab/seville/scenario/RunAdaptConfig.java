package org.sutlab.seville.scenario;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.config.groups.VehiclesConfigGroup;
import org.sutlab.seville.SevilleConfigurator;
import org.sutlab.seville.mode_choice.SevilleModeChoiceModule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RunAdaptConfig {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args).allowAnyOption(true).build();
		ConfigAdapter.run(args, new SevilleConfigurator(cmd), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config, String prefix) {
		// MATSim: routing
		config.routing().setAccessEgressType(AccessEgressType.accessEgressModeToLink);

		Set<String> networkModes = new HashSet<>(config.routing().getNetworkModes());
		networkModes.add(SevilleModeChoiceModule.CAR_PASSENGER);
		config.routing().setNetworkModes(networkModes);

		TeleportedModeParams bicycleRouteParams = config.routing().getTeleportedModeParams().get(TransportMode.bike);
		bicycleRouteParams.setTeleportedModeSpeed(15.0 / 3.6);
		bicycleRouteParams.setBeelineDistanceFactor(1.3);
		
		TeleportedModeParams walkRouteParams = config.routing().getTeleportedModeParams().get(TransportMode.walk);
		walkRouteParams.setTeleportedModeSpeed(4.5 / 3.6);
		walkRouteParams.setBeelineDistanceFactor(1.3);

		// MATSim: scoring
		for (String mode : Arrays.asList("bike", "passenger")) {
			ModeParams modeScoringParams = new ModeParams(mode);
			modeScoringParams.setMarginalUtilityOfTraveling(-1.0);
			config.scoring().addModeParams(modeScoringParams);
		}

		// Adjust eqasim config
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setCostModel(TransportMode.car, SevilleModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, SevilleModeChoiceModule.PT_COST_MODEL_NAME);

		eqasimConfig.setEstimator(TransportMode.car, SevilleModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.pt, SevilleModeChoiceModule.PT_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.bike, SevilleModeChoiceModule.BICYCLE_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(SevilleModeChoiceModule.CAR_PASSENGER, SevilleModeChoiceModule.CAR_PASSENGER_ESTIMATOR_NAME);

		// Discrete mode choice
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(SevilleModeChoiceModule.MODE_AVAILABILITY_NAME);
		dmcConfig.setCachedModes(Arrays.asList("car", "bike", "pt", "walk", SevilleModeChoiceModule.CAR_PASSENGER, "truck"));

		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.remove(EqasimModeChoiceModule.PASSENGER_CONSTRAINT_NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		VehicleTourConstraintConfigGroup vehicleTourConstraint = dmcConfig.getVehicleTourConstraintConfig();
		vehicleTourConstraint.setRestrictedModes(Arrays.asList("car", "bike"));

		// Major crossing penalty from calibration
		eqasimConfig.setCrossingPenalty(4.2);

		// Setting this as done in Epsilon AdaptConfigForEpsilon.run(config) as it has been deprecated
		dmcConfig.setSelector(SelectorModule.MAXIMUM);
		eqasimConfig.setUsePseudoRandomErrors(true);


		// Vehicles
		QSimConfigGroup qsimConfig = config.qsim();
		qsimConfig.setVehiclesSource(VehiclesSource.fromVehiclesData);

		VehiclesConfigGroup vehiclesConfig = config.vehicles();
		vehiclesConfig.setVehiclesFile(prefix + "vehicles.xml.gz");

		// Convergence
		EqasimTerminationConfigGroup terminationConfig = EqasimTerminationConfigGroup.getOrCreate(config);
		terminationConfig.setModes(Arrays.asList("car", "car_passenger", "pt", "bike", "walk"));
	}
}