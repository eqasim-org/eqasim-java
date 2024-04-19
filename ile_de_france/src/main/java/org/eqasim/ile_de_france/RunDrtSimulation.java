package org.eqasim.ile_de_france;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.eqasim.core.components.transit.EqasimTransitQSimModule;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.modes.drt.utils.AdaptConfigForDrt;
import org.eqasim.ile_de_france.analysis.counts.CountsModule;
import org.eqasim.ile_de_france.analysis.delay.DelayAnalysisModule;
import org.eqasim.ile_de_france.analysis.urban.UrbanAnalysisModule;
import org.eqasim.ile_de_france.drt.IDFDrtCostModel;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.parking.ParkingModule;
import org.eqasim.ile_de_france.routing.IDFRaptorModule;
import org.eqasim.ile_de_france.routing.IDFRaptorUtils;
import org.eqasim.ile_de_france.scenario.RunAdaptConfig;
import org.eqasim.vdf.VDFConfigGroup;
import org.eqasim.vdf.VDFModule;
import org.eqasim.vdf.VDFQSimModule;
import org.eqasim.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.vdf.engine.VDFEngineModule;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contribs.discrete_mode_choice.components.constraints.ShapeFileConstraint.Requirement;
import org.matsim.contribs.discrete_mode_choice.modules.ConstraintModule;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;

// TODO: Operating area , adjust cost model , adjust drt parameters , add prebooking

public class RunDrtSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "vehicles") //
				.allowOptions("counts-path", "use-epsilon", "use-vdf", "use-vdf-engine", "vdf-generate-network-events",
						"line-switch-utility", "cost-model") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", OsmNetworkAdjustment.CAPACITY_PREFIX,
						OsmNetworkAdjustment.SPEED_PREFIX, "raptor") //
				.build();

		boolean useVdf = cmd.getOption("use-vdf").map(Boolean::parseBoolean).orElse(false);
		IDFConfigurator configurator = new IDFConfigurator();

		if (useVdf) {
			configurator.getQSimModules().removeIf(m -> m instanceof EqasimTrafficQSimModule);
		}

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		config.addModule(new VDFConfigGroup());
		configurator.addOptionalConfigGroups(config);

		AdaptConfigForDrt.adapt(config, Collections.singletonMap("drt", "NOT USED"),
				Collections.singletonMap("drt", DrtConfigGroup.OperationalScheme.door2door.name()),
				Collections.emptyMap(), Collections.emptyMap(), String.valueOf(30.0 * 3600.0), null);

		DiscreteModeChoiceConfigGroup dmcConfig2 = DiscreteModeChoiceConfigGroup.getOrCreate(config);

		Set<String> tripConstraints = new HashSet<>(dmcConfig2.getTripConstraints());
		tripConstraints.add(EqasimModeChoiceModule.DRT_WALK_CONSTRAINT);
		tripConstraints.add(ConstraintModule.SHAPE_FILE);
		dmcConfig2.setTripConstraints(tripConstraints);

		dmcConfig2.getShapeFileConstraintConfigGroup().setConstrainedModes(Collections.singleton("drt"));
		dmcConfig2.getShapeFileConstraintConfigGroup().setPath("drt_area.shp");
		dmcConfig2.getShapeFileConstraintConfigGroup().setRequirement(Requirement.BOTH);

		EqasimConfigGroup eqasimConfig2 = EqasimConfigGroup.get(config);
		eqasimConfig2.setCostModel("drt", "IDFDrtCostModel");

		cmd.applyConfiguration(config);

		{
			// Avoid logging errors when using TripsAndLegsCSV
			config.controler().setWriteTripsInterval(0);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		{
			config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(-1.0);
			IDFRaptorUtils.updateScoring(config);
		}

		new OsmNetworkAdjustment(cmd).apply(config, scenario.getNetwork());

		RunAdaptConfig.adaptEstimators(config);
		RunAdaptConfig.adaptConstraints(config);

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new UrbanAnalysisModule());
		controller.addOverridingModule(new DelayAnalysisModule());

		controller.addOverridingModule(new AbstractEqasimExtension() {
			@Override
			protected void installEqasimExtension() {
				bindCostModel("IDFDrtCostModel").to(IDFDrtCostModel.class);
			}
		});

		if (cmd.hasOption("line-switch-utility")) {
			double lineSwitchUtility = Double.parseDouble(cmd.getOptionStrict("line-switch-utility"));
			config.planCalcScore().setUtilityOfLineSwitch(lineSwitchUtility);
		}

		if (cmd.hasOption("counts-path")) {
			controller.addOverridingModule(new CountsModule(cmd));
		}

		if (cmd.getOption("use-epsilon").map(Boolean::parseBoolean).orElse(true)) {
			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
			dmcConfig.setSelector(SelectorModule.MAXIMUM);

			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

			eqasimConfig.setEstimator("car", "epsilon_" + IDFModeChoiceModule.CAR_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("pt", "epsilon_" + IDFModeChoiceModule.PT_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("bike", "epsilon_" + IDFModeChoiceModule.BIKE_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("walk", "epsilon_" + EqasimModeChoiceModule.WALK_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("car_passenger", "epsilon_" + IDFModeChoiceModule.PASSENGER_ESTIMATOR_NAME);
			eqasimConfig.setEstimator("drt", "epsilon_" + EqasimModeChoiceModule.DRT_ESTIMATOR_NAME);
		}

		if (useVdf) {
			controller.addOverridingModule(new VDFModule());
			controller.addOverridingQSimModule(new VDFQSimModule());

			config.qsim().setFlowCapFactor(1e9);
			config.qsim().setStorageCapFactor(1e9);
		}

		boolean useVdfEngine = cmd.getOption("use-vdf-engine").map(Boolean::parseBoolean).orElse(false);
		if (useVdfEngine) {
			Set<String> mainModes = new HashSet<>(config.qsim().getMainModes());
			mainModes.remove("car");
			config.qsim().setMainModes(mainModes);

			VDFEngineConfigGroup vdfEngineConfig = new VDFEngineConfigGroup();
			config.addModule(vdfEngineConfig);

			vdfEngineConfig.setGenerateNetworkEvents(
					cmd.getOption("vdf-generate-network-events").map(Boolean::parseBoolean).orElse(true));
			vdfEngineConfig.setModes(Collections.singleton("car"));

			controller.addOverridingModule(new VDFEngineModule());

			controller.configureQSimComponents(cfg -> {
				EqasimTransitQSimModule.configure(cfg, controller.getConfig());
				cfg.addNamedComponent(VDFEngineModule.COMPONENT_NAME);
			});
		}

		controller.addOverridingModule(new ParkingModule(3.0));
		controller.addOverridingModule(new IDFRaptorModule(cmd));

		Set<Geometry> shapes = new HashSet<>();

		try {
			URL url = ConfigGroup.getInputFileURL(config.getContext(), "drt_area.shp");
			DataStore dataStore = DataStoreFinder.getDataStore(Collections.singletonMap("url", url));

			SimpleFeatureSource featureSource = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
			SimpleFeatureCollection featureCollection = featureSource.getFeatures();
			SimpleFeatureIterator featureIterator = featureCollection.features();

			while (featureIterator.hasNext()) {
				SimpleFeature feature = featureIterator.next();
				shapes.add((Geometry) feature.getDefaultGeometry());
			}

			featureIterator.close();
			dataStore.dispose();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		GeometryFactory geometryFactory = new GeometryFactory();
		List<Link> candidates = new LinkedList<>();

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				for (Geometry shape : shapes) {
					Coord coord = link.getCoord();
					Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
					Point point = geometryFactory.createPoint(coordinate);

					if (shape.contains(point)) {
						candidates.add(link);

						Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
						allowedModes.add("drt");
						link.setAllowedModes(allowedModes);
					}
				}
			}
		}
		
		final int vehicles = Integer.parseInt(cmd.getOptionStrict("vehicles"));
		
		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(FleetSpecification.class).toProvider(modalProvider(getter -> {
					FleetSpecificationImpl fleetSpecification = new FleetSpecificationImpl();

					Random random = new Random(0);
					List<Link> candidates = new LinkedList<>(getter.getModal(Network.class).getLinks().values());

					for (int i = 0; i < vehicles; i++) {
						Link startLink = candidates.get(random.nextInt(candidates.size()));

						fleetSpecification.addVehicleSpecification(ImmutableDvrpVehicleSpecification.newBuilder() //
								.id(Id.create("drt_" + i, DvrpVehicle.class)) //
								.capacity(8) //
								.serviceBeginTime(0.0) //
								.serviceEndTime(30.0 * 3600.0) //
								.startLinkId(startLink.getId()) //
								.build());
					}

					return fleetSpecification;
				}));
			}
		});

		DvrpConfigGroup.get(config).networkModes = Collections.singleton("drt");

		DrtConfigGroup
				.getSingleModeDrtConfig(config).operationalScheme = DrtConfigGroup.OperationalScheme.serviceAreaBased;
		DrtConfigGroup.getSingleModeDrtConfig(config).drtServiceAreaShapeFile = "drt_area.shp";
		DrtConfigGroup.getSingleModeDrtConfig(config).useModeFilteredSubnetwork = true;

		controller.run();
	}
}