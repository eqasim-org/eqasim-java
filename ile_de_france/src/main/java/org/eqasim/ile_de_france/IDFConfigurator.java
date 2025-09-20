package org.eqasim.ile_de_france;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;

import java.util.*;

public class IDFConfigurator extends EqasimConfigurator {
    public IDFConfigurator(CommandLine cmd) {
        super(cmd);

        registerModule(new IDFModeChoiceModule(cmd));
    }

    @Override
    public void adjustScenario(Scenario scenario) {
        super.adjustScenario(scenario);

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

            // TODO : do we need to add motorcycles to this ?
            // 	<module name="eqasim:termination" >
            //		<param name="modes" value="walk,bike,pt,car,car_passenger,truck,outside" />
        }
		{ // Scoring
			ScoringConfigGroup.ModeParams modeParameters = new ScoringConfigGroup.ModeParams("motorcycle");
			modeParameters.setConstant(0.0);
			modeParameters.setMarginalUtilityOfDistance(0.0);
			modeParameters.setMonetaryDistanceRate(0.0);
			config.scoring().addModeParams(modeParameters);
		}
        { // Routing
            List<String> networkModes = new ArrayList<>(config.routing().getNetworkModes());
            networkModes.add("motorcycle");
            config.routing().setNetworkModes(networkModes);
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
        { // Population ; force motorcycle starting mode
//            Population population = scenario.getPopulation();
//            for (Person person : population.getPersons().values()) {
//                String motorcycleAvailability = (String) person.getAttributes().getAttribute("motorcycleAvailability");
//                if ("some".equals(motorcycleAvailability) | "all".equals(motorcycleAvailability)) {
//                    Plan plan = person.getSelectedPlan();
//                    for (PlanElement element : plan.getPlanElements()) {
//                        if (element instanceof Leg) {
//                            Leg leg = (Leg) element;
//                            leg.setMode(TransportMode.motorcycle);
//                            leg.setRoutingMode(TransportMode.motorcycle);
//                        }
//                    }
//                }
//            }
        }
    }
}
