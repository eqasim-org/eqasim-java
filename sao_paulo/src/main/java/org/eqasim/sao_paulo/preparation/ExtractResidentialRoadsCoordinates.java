package org.eqasim.sao_paulo.preparation;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class ExtractResidentialRoadsCoordinates {

	public static void main(String[] args) throws IOException {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		MatsimNetworkReader networReader = new MatsimNetworkReader(scenario.getNetwork());
		networReader.readFile(args[0]);
		BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
		writer.write("coordX,coordY,purpose\n");
		int countS = 0;
		int countE = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {

			if (link.getAttributes().getAsMap().containsKey("osm:way:highway")
					&& ((String) link.getAttributes().getAsMap().get("osm:way:highway")).equals("residential")) {
				if (countS % 25 == 0) {
					writer.write(link.getCoord().getX() + "," + link.getCoord().getY() + ",shop\n");
					writer.write(link.getCoord().getX() + "," + link.getCoord().getY() + ",leisure\n");

				}
				if (countE % 100 == 0) {
					writer.write(link.getCoord().getX() + "," + link.getCoord().getY() + ",education\n");
				}
				countS++;
				countE++;
			}

		}
		writer.flush();
		writer.close();
	}

}
