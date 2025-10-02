package org.eqasim.ile_de_france.scenario;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.epsilon.AdaptConfigForEpsilon;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
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

public class RunAdaptConfig {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args).allowAnyOption(true).build();
		ConfigAdapter.run(args, new IDFConfigurator(cmd), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config, String prefix) {
		// MATSim: routing
		config.routing().setAccessEgressType(AccessEgressType.accessEgressModeToLink);

		Set<String> networkModes = new HashSet<>(config.routing().getNetworkModes());
		networkModes.add(IDFModeChoiceModule.CAR_PASSENGER);
		config.routing().setNetworkModes(networkModes);

		TeleportedModeParams bicycleRouteParams = new TeleportedModeParams();
		bicycleRouteParams.setMode("bicycle");
		bicycleRouteParams.setTeleportedModeSpeed(9.1 / 3.6);
		bicycleRouteParams.setBeelineDistanceFactor(1.3);
		config.routing().addTeleportedModeParams(bicycleRouteParams);

		TeleportedModeParams walkRouteParams = config.routing().getTeleportedModeParams().get(TransportMode.walk);
		walkRouteParams.setTeleportedModeSpeed(3.25 / 3.6);
		walkRouteParams.setBeelineDistanceFactor(1.3);

		// MATSim: scoring
		for (String mode : Arrays.asList(IDFModeChoiceModule.BICYCLE, IDFModeChoiceModule.CAR_PASSENGER)) {
			ModeParams modeScoringParams = new ModeParams(mode);
			modeScoringParams.setMarginalUtilityOfTraveling(-1.0);
			config.scoring().addModeParams(modeScoringParams);
		}

		// Adjust eqasim config
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setCostModel(TransportMode.car, IDFModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, IDFModeChoiceModule.PT_COST_MODEL_NAME);

		eqasimConfig.setEstimator(TransportMode.car, IDFModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.pt, IDFModeChoiceModule.PT_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(IDFModeChoiceModule.BICYCLE, IDFModeChoiceModule.BICYCLE_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(IDFModeChoiceModule.CAR_PASSENGER, IDFModeChoiceModule.CAR_PASSENGER_ESTIMATOR_NAME);
		eqasimConfig.removeEstimator(TransportMode.bike);

		// Discrete mode choice
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(IDFModeChoiceModule.MODE_AVAILABILITY_NAME);
		dmcConfig.setCachedModes(Arrays.asList("car", IDFModeChoiceModule.BICYCLE, "pt", "walk",
				IDFModeChoiceModule.CAR_PASSENGER, "truck"));

		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.remove(EqasimModeChoiceModule.PASSENGER_CONSTRAINT_NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		VehicleTourConstraintConfigGroup vehicleTourConstraint = dmcConfig.getVehicleTourConstraintConfig();
		vehicleTourConstraint.setRestrictedModes(Arrays.asList("car", IDFModeChoiceModule.BICYCLE));

		// Major crossing penalty from calibration
		eqasimConfig.setCrossingPenalty(0.0);

		// Epsilon
		AdaptConfigForEpsilon.run(config);

		// Vehicles
		QSimConfigGroup qsimConfig = config.qsim();
		qsimConfig.setVehiclesSource(VehiclesSource.fromVehiclesData);

		VehiclesConfigGroup vehiclesConfig = config.vehicles();
		vehiclesConfig.setVehiclesFile(prefix + "vehicles.xml.gz");

		// Convergence
		EqasimTerminationConfigGroup terminationConfig = EqasimTerminationConfigGroup.getOrCreate(config);
		terminationConfig.setModes(
				Arrays.asList("car", IDFModeChoiceModule.CAR_PASSENGER, "pt", IDFModeChoiceModule.BICYCLE, "walk"));
	}
}
