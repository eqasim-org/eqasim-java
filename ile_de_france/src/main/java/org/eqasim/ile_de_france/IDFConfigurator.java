package org.eqasim.ile_de_france;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

public class IDFConfigurator extends EqasimConfigurator {
    public void adjustScenario(Scenario scenario) {
        // if there is a vehicles file defined in config, manually assign them to their agents
        Config config = scenario.getConfig();
        if (config.qsim().getVehiclesSource() == QSimConfigGroup.VehiclesSource.fromVehiclesData) {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                Id<Vehicle> carId = Id.createVehicleId(person.getId());
                Id<Vehicle> motorcycleId = Id.createVehicleId(String.format("m_%s", person.getId()));
                Map<String, Id<Vehicle>> modeVehicle = new HashMap<>();
                modeVehicle.put("car", carId);
                if (person.getAttributes().getAsMap().containsKey("motorcycleAvailability")) {
                    if (!((String) person.getAttributes().getAttribute("motorcycleAvailability")).equals("none")) {
                        modeVehicle.put("motorcycle", motorcycleId);
                    }
                }
                VehicleUtils.insertVehicleIdsIntoAttributes(person, modeVehicle);
            }
        }
    }

    public void adjustScenarioMotorcycle(Scenario scenario) {
        // add motorcycle stuff
        Config config = scenario.getConfig();
        { // QSim
            List<String> qsimMainModes = new ArrayList<>(config.qsim().getMainModes());
            qsimMainModes.add("motorcycle");
            config.qsim().setMainModes(qsimMainModes);

            config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.SeepageQ);

            List<String> qsimSeepModes = new ArrayList<>(config.qsim().getSeepModes());
            qsimSeepModes.add("motorcycle");
            config.qsim().setSeepModes(qsimSeepModes);
        }
        { // DMC
            DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
                    .get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
            Collection<String> cachedModes = dmcConfig.getCachedModes();
            cachedModes.add("motorcycle");
            dmcConfig.setCachedModes(cachedModes);

            List<String> vehicleConstraintModes = new ArrayList<>(dmcConfig.getVehicleTourConstraintConfig().getRestrictedModes());
            vehicleConstraintModes.add("motorcycle");
            dmcConfig.getVehicleTourConstraintConfig().setRestrictedModes(vehicleConstraintModes);
        }
        { // Eqasim
            EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
            eqasimConfig.setEstimator(TransportMode.motorcycle, IDFModeChoiceModule.MOTORCYCLE_ESTIMATOR_NAME);
            eqasimConfig.setCostModel(TransportMode.motorcycle, IDFModeChoiceModule.MOTORCYCLE_COST_MODEL_NAME);
        }
        { // planCalcScore
            PlanCalcScoreConfigGroup.ModeParams modeParameters = new PlanCalcScoreConfigGroup.ModeParams("motorcycle");
            modeParameters.setConstant(0.0);
            modeParameters.setMarginalUtilityOfDistance(0.0);
            modeParameters.setMonetaryDistanceRate(0.0);
            config.planCalcScore().addModeParams(modeParameters);

            config.planCalcScore().setWriteExperiencedPlans(true);
        }
        { // plansCalcRoute
            List<String> networkModes = new ArrayList<>(config.plansCalcRoute().getNetworkModes());
            networkModes.add("motorcycle");
            config.plansCalcRoute().setNetworkModes(networkModes);
        }
        { // Network
            Network network = scenario.getNetwork();
            for (Link link : network.getLinks().values()) {
                Set<String> modes = new HashSet<>(link.getAllowedModes());
                if (modes.contains("car")) {
                    modes.add("motorcycle");
                    link.setAllowedModes(modes);
                }
            }
        }
    }
}
