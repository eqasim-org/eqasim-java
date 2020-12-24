package org.eqasim.automated_vehicles.components;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.automated_vehicles.mode_choice.AvModeChoiceModule;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.opengis.feature.simple.SimpleFeature;

import ch.ethz.matsim.av.config.AVConfigGroup;
import ch.ethz.matsim.av.config.operator.DispatcherConfig;
import ch.ethz.matsim.av.config.operator.GeneratorConfig;
import ch.ethz.matsim.av.config.operator.OperatorConfig;
import ch.ethz.matsim.av.config.operator.WaitingTimeConfig;
import ch.ethz.matsim.av.dispatcher.single_heuristic.SingleHeuristicDispatcher;
import ch.ethz.matsim.av.framework.AVModule;
import ch.ethz.matsim.av.generator.PopulationDensityGenerator;

public final class AvConfigurator {
	private AvConfigurator() {
	}

	static public void configure(Config config) {
		// Set up DVRP
		if (!config.getModules().containsKey(DvrpConfigGroup.GROUP_NAME)) {
			config.addModule(new DvrpConfigGroup());
		}

		// Set up AV extension
		if (!config.getModules().containsKey(AVConfigGroup.GROUP_NAME)) {
			AVConfigGroup avConfig = AVConfigGroup.getOrCreate(config);
			avConfig.setAllowedLinkMode("av");

			// Set up operator
			OperatorConfig operatorConfig = new OperatorConfig();
			avConfig.addOperator(operatorConfig);
			operatorConfig.setPredictRouteTravelTime(true); // Important for prediction!

			DispatcherConfig dispatcherConfig = operatorConfig.getDispatcherConfig();
			dispatcherConfig.setType(SingleHeuristicDispatcher.TYPE);

			GeneratorConfig generatorConfig = operatorConfig.getGeneratorConfig();
			generatorConfig.setType(PopulationDensityGenerator.TYPE);
			generatorConfig.setNumberOfVehicles(10);

			WaitingTimeConfig waitingTimeConfig = operatorConfig.getWaitingTimeConfig();
			waitingTimeConfig.setEstimationAlpha(0.1);
		}

		// Set up DiscreteModeChoice
		DiscreteModeChoiceConfigGroup dmcConfig = getOrCreateDiscreteModeChoiceConfigGroup(config);

		// -- Constraint that makes sure we don't have "fallback" trips for AV that only
		// consist of walk
		Set<String> tripConstraints = new HashSet<>();
		tripConstraints.addAll(dmcConfig.getTripConstraints());
		tripConstraints.add(AvModeChoiceModule.AV_WALK_CONSTRAINT_NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		// -- Add AV to the cached modes
		Set<String> cachedModes = new HashSet<>();
		cachedModes.addAll(dmcConfig.getCachedModes());
		cachedModes.add("av");
		dmcConfig.setCachedModes(cachedModes);

		// Set up MATSim scoring (although we don't really use it - MATSim wants it)
		ModeParams modeParams = new ModeParams(AVModule.AV_MODE);
		config.planCalcScore().addModeParams(modeParams);
		
		ActivityParams interactionParams = new ActivityParams("av interaction");
		interactionParams.setTypicalDuration(3600.0);
		config.planCalcScore().addActivityParams(interactionParams);

		// Set up Eqasim (add AV cost model and estimator)
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		eqasimConfig.setCostModel("av", AvModeChoiceModule.AV_COST_MODEL_NAME);
		eqasimConfig.setEstimator("av", AvModeChoiceModule.AV_ESTIMATOR_NAME);

		// Set up config group
		EqasimAvConfigGroup.getOrCreate(config);
	}

	// TODO: This will be included in the next version of DMC
	static public DiscreteModeChoiceConfigGroup getOrCreateDiscreteModeChoiceConfigGroup(Config config) {
		DiscreteModeChoiceConfigGroup configGroup = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		if (configGroup == null) {
			configGroup = new DiscreteModeChoiceConfigGroup();
			config.addModule(configGroup);
		}

		return configGroup;
	}

	static public void configureCarLinks(Scenario scenario) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> allowedModes = new HashSet<>(link.getAllowedModes());

			if (allowedModes.contains("car")) {
				allowedModes.add("av");
			}

			link.setAllowedModes(allowedModes);
		}
	}

	static public void configureUniformWaitingTimeGroup(Scenario scenario) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.getAttributes().putAttribute("avWaitingTimeGroup", 0);
		}
	}
	
	static public void configureWaitingTimeGroupFromShapefile(File path, String attributeName, Network network) {
		GeometryFactory geometryFactory = new GeometryFactory();
		Map<Integer, Geometry> shapes = new HashMap<>();

		try {
			DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", path.toURI().toURL()));

			SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();
			SimpleFeatureIterator featureIterator = featureCollection.features();

			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				int index = (int) (long) (Long) feature.getAttribute(attributeName);
				shapes.put(index, (Geometry) feature.getDefaultGeometry());
			}

			featureIterator.close();
			dataStore.dispose();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		for (Link link : network.getLinks().values()) {
			Coordinate coordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
			Point point = geometryFactory.createPoint(coordinate);

			for (Map.Entry<Integer, Geometry> entry : shapes.entrySet()) {
				if (entry.getValue().contains(point)) {
					link.getAttributes().putAttribute("avWaitingTimeGroup", entry.getKey());
				}
			}
		}
	}

	static public void configureController(Controler controller, CommandLine cmd) {
		controller.addOverridingModule(new AVModule());
		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new AvModeChoiceModule(cmd));
	}
}
