package org.eqasim.server.backend;

import java.io.File;
import java.util.Collections;

import org.eqasim.server.grid.GridSource;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class BackendScenario {
	private Config config;

	private Network network;
	private Network carNetwork;

	private TransitSchedule schedule;

	private GridSource gridSource;

	public Config getConfig() {
		return config;
	}

	public Network getNetwork() {
		return network;
	}

	public Network getCarNetwork() {
		return carNetwork;
	}

	public TransitSchedule getSchedule() {
		return schedule;
	}

	public GridSource getGridSource() {
		return gridSource;
	}

	static public BackendScenario create(File configPath) {
		Config config = ConfigUtils.loadConfig(configPath.toString());
		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario.getNetwork())
				.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.network().getInputFile()));

		Network carNetwork = NetworkUtils.createNetwork(config.network());
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(carNetwork,
				Collections.singleton(TransportMode.car));

		new TransitScheduleReader(scenario)
				.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.transit().getTransitScheduleFile()));

		GridSource gridSource = new GridSource.Builder() //
				.add(carNetwork.getNodes().values()) //
				.add(scenario.getTransitSchedule().getFacilities().values()) //
				.build();

		BackendScenario backend = new BackendScenario();

		backend.config = config;
		backend.network = scenario.getNetwork();
		backend.carNetwork = carNetwork;
		backend.schedule = scenario.getTransitSchedule();
		backend.gridSource = gridSource;

		return backend;
	}
}
