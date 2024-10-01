package org.eqasim.ile_de_france.scenario;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.mode_choice.epsilon.AdaptConfigForEpsilon;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
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
		IDFConfigurator configurator = new IDFConfigurator();
		ConfigAdapter.run(args, configurator.getConfigGroups(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config, String prefix) {
		// MATSim: routing
		config.routing().setAccessEgressType(AccessEgressType.accessEgressModeToLink);
		
		Set<String> networkModes = new HashSet<>(config.routing().getNetworkModes());
		networkModes.add("passenger");
		networkModes.add("motorbike");
		config.routing().setNetworkModes(networkModes);
		
		// MATSim: scoring
		TeleportedModeParams bicycleRouteParams = new TeleportedModeParams();
		bicycleRouteParams.setMode("bicycle");
		bicycleRouteParams.setTeleportedModeSpeed(15.0 / 3.6);
		bicycleRouteParams.setBeelineDistanceFactor(1.3);
		config.routing().addTeleportedModeParams(bicycleRouteParams);
		
		for (String mode : Arrays.asList("motorbike", "bicycle", "passenger")) {
			ModeParams modeScoringParams = new ModeParams(mode);
			modeScoringParams.setMarginalUtilityOfTraveling(-1.0);
			config.scoring().addModeParams(modeScoringParams);
		}
		
		// Adjust eqasim config
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setCostModel(TransportMode.car, IDFModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, IDFModeChoiceModule.PT_COST_MODEL_NAME);
		eqasimConfig.setCostModel(IDFModeChoiceModule.MOTORBIKE, IDFModeChoiceModule.MOTORBIKE_COST_MODEL_NAME);

		eqasimConfig.setEstimator(TransportMode.car, IDFModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(IDFModeChoiceModule.BICYCLE, IDFModeChoiceModule.BICYCLE_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(IDFModeChoiceModule.MOTORBIKE, IDFModeChoiceModule.MOTORBIKE_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(IDFModeChoiceModule.PASSENGER, IDFModeChoiceModule.PASSENGER_ESTIMATOR_NAME);
		eqasimConfig.removeEstimator(TransportMode.bike);
		
		// Discrete mode choice
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(IDFModeChoiceModule.MODE_AVAILABILITY_NAME);
		
		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.remove(EqasimModeChoiceModule.PASSENGER_CONSTRAINT_NAME);
		dmcConfig.setTripConstraints(tripConstraints);
		
		VehicleTourConstraintConfigGroup vehicleTourConstraint = dmcConfig.getVehicleTourConstraintConfig();
		vehicleTourConstraint.setRestrictedModes(Arrays.asList("car", "bicycle", "motorbike"));
		
		// Major crossing penalty from calibration
		eqasimConfig.setCrossingPenalty(4.2);
		
		// Epsilon
		AdaptConfigForEpsilon.run(config);
		
		// Vehicles
		QSimConfigGroup qsimConfig = config.qsim();
		qsimConfig.setVehiclesSource(VehiclesSource.fromVehiclesData);
		
		VehiclesConfigGroup vehiclesConfig = config.vehicles();
		vehiclesConfig.setVehiclesFile(prefix + "vehicles.xml.gz");
	}
}
