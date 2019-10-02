package org.eqasim.projects.dynamic_av;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.calibration.CalibrationConfigGroup;
import org.eqasim.projects.dynamic_av.service_area.OperatingArea;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.households.Household;

import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

public class ProjectConfigurator extends EqasimConfigurator {
	private ProjectConfigurator() {
	}

	static public ConfigGroup[] getConfigGroups() {
		return new ConfigGroup[] { //
				new SwissRailRaptorConfigGroup(), //
				new EqasimConfigGroup(), //
				new DiscreteModeChoiceConfigGroup(), //
				new CalibrationConfigGroup(), //
				new ProjectConfigGroup() //
		};
	}

	static public void configure(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setEstimator(TransportMode.car, ProjectModule.PROJECT_CAR_ESTIMATOR);
		eqasimConfig.setEstimator(TransportMode.pt, ProjectModule.PROJECT_PT_ESTIMATOR);
		eqasimConfig.setEstimator(TransportMode.bike, ProjectModule.PROJECT_BIKE_ESTIMATOR);
		eqasimConfig.setEstimator(TransportMode.walk, ProjectModule.PROJECT_WALK_ESTIMATOR);
		eqasimConfig.setEstimator(AVModule.AV_MODE, ProjectModule.PROJECT_AV_ESTIMATOR);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
		dmcConfig.setModeAvailability(ProjectModule.PROJECT_MODE_AVAILABILITY_NAME);
	}

	static public void adjustScenario(Scenario scenario) {
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					person.getAttributes().putAttribute("householdIncome", household.getIncome().getIncome());
				}
			}
		}
	}

	static public void adjustNetwork(Scenario scenario) {
		ProjectConfigGroup projectConfig = ProjectConfigGroup.get(scenario.getConfig());
		Network network = scenario.getNetwork();

		OperatingArea operatingArea = OperatingArea.load(projectConfig.getWaitingTimeGroupIndexAttribute(), network,
				ConfigGroup.getInputFileURL(scenario.getConfig().getContext(), projectConfig.getOperatingAreaPath()));
		
		OperatorConfig operatorConfig = AVConfigGroup.getOrCreate(scenario.getConfig())
				.getOperatorConfig(OperatorConfig.DEFAULT_OPERATOR_ID);
		
		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				modes.add(AVModule.AV_MODE);
				link.setAllowedModes(modes);
			}
			
			if (operatingArea.covers(link)) {
				link.getAttributes().putAttribute(operatorConfig.getAllowedLinkAttribute(), true);
			}
		}
	}
}
