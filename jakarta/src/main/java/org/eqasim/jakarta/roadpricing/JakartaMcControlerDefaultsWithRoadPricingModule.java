/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RoadPricingModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.eqasim.jakarta.roadpricing;

import java.net.URL;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

public final class JakartaMcControlerDefaultsWithRoadPricingModule extends AbstractModule {

	final JakartaMcRoadPricingScheme roadPricingScheme;

	public JakartaMcControlerDefaultsWithRoadPricingModule() {
		this.roadPricingScheme = null;
	}

	public JakartaMcControlerDefaultsWithRoadPricingModule(JakartaMcRoadPricingScheme roadPricingScheme) {
		this.roadPricingScheme = roadPricingScheme;
	}

	@Override
	public void install() {
		// This is not optimal yet. Modules should not need to have parameters.
		// But I am not quite sure yet how to best handle custom scenario elements. mz

		// use ControlerDefaults configuration, replacing the TravelDisutility with a toll-dependent one
		install(AbstractModule.override(Arrays.<AbstractModule>asList(new ControlerDefaultsModule()), new JakartaMcRoadPricingModule(roadPricingScheme)));
	}

	static class RoadPricingInitializer {
		@Inject
		RoadPricingInitializer(JakartaMcRoadPricingScheme roadPricingScheme, Scenario scenario) {
			JakartaMcRoadPricingScheme scenarioRoadPricingScheme = (JakartaMcRoadPricingScheme) scenario.getScenarioElement(JakartaMcRoadPricingScheme.ELEMENT_NAME);
			if (scenarioRoadPricingScheme == null) {
				scenario.addScenarioElement(JakartaMcRoadPricingScheme.ELEMENT_NAME, roadPricingScheme);
			} else {
				if (roadPricingScheme != scenarioRoadPricingScheme) {
					throw new RuntimeException();
				}
			}
		}
	}


	static class RoadPricingSchemeProvider implements Provider<JakartaMcRoadPricingScheme> {

		private final Config config;
		private Scenario scenario;

		@Inject
		RoadPricingSchemeProvider(Config config, Scenario scenario) {
			this.config = config;
			this.scenario = scenario;
		}

		@Override
		public JakartaMcRoadPricingScheme get() {
			JakartaMcRoadPricingScheme scenarioRoadPricingScheme = (JakartaMcRoadPricingScheme) scenario.getScenarioElement(JakartaMcRoadPricingScheme.ELEMENT_NAME);
			if (scenarioRoadPricingScheme != null) {
				return scenarioRoadPricingScheme;
			} else {
				JakartaMcRoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, JakartaMcRoadPricingConfigGroup.GROUP_NAME, JakartaMcRoadPricingConfigGroup.class);

				if ( rpConfig.getTollLinksFile() == null ) {
					throw new RuntimeException("Road pricing inserted but neither toll links file nor RoadPricingScheme given.  "
							+ "Such an execution path is not allowed.  If you want a base case without toll, "
							+ "construct a zero toll file and insert that. ") ;
				}
				URL tollLinksFile = ConfigGroup.getInputFileURL(this.config.getContext(), rpConfig.getTollLinksFile());
				JakartaMcRoadPricingSchemeImpl rpsImpl = new JakartaMcRoadPricingSchemeImpl() ;
				new JakartaMcRoadPricingReaderXMLv1(rpsImpl).parse(tollLinksFile);
				return rpsImpl;
			}
		}
	}

	static class TravelDisutilityIncludingTollFactoryProvider implements Provider<TravelDisutilityFactory> {

		private final Scenario scenario;
		private final JakartaMcRoadPricingScheme scheme;

		@Inject
		TravelDisutilityIncludingTollFactoryProvider(Scenario scenario, JakartaMcRoadPricingScheme scheme) {
			this.scenario = scenario;
			this.scheme = scheme;
		}

		@Override
		public TravelDisutilityFactory get() {
			final Config config = scenario.getConfig();
			final TravelDisutilityFactory originalTravelDisutilityFactory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
			//			if (!scheme.getType().equals(RoadPricingScheme.TOLL_TYPE_AREA)) {
			JakartaMcRoadPricingTravelDisutilityFactory travelDisutilityFactory = new JakartaMcRoadPricingTravelDisutilityFactory(
					originalTravelDisutilityFactory, scheme, config.planCalcScore().getMarginalUtilityOfMoney()
					);
			travelDisutilityFactory.setSigma(config.plansCalcRoute().getRoutingRandomness());
			return travelDisutilityFactory;
			//            } else {
			//                return originalTravelDisutilityFactory;
			//            }
		}

	}

}
