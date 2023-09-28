package org.eqasim.core.tools.routing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

public class LinkRouter {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "input-path", "output-path") //
				.build();

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(roadNetwork, Collections.singleton("car"));

		BufferedReader inputReader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(cmd.getOptionStrict("input-path")))));

		List<Id<Link>> linkIds = Arrays.asList(inputReader.readLine().split(";")).stream().map(Id::createLinkId)
				.collect(Collectors.toList());

		inputReader.close();

		TravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathCalculator router = new SpeedyALTFactory().createPathCalculator(roadNetwork,
				new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

		BufferedWriter outputWriter = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(new File(cmd.getOptionStrict("output-path")))));

		outputWriter.write(String.join(";", new String[] { //
				"from_link", "to_link", "distance", "sequence" //
		}) + "\n");

		int total = linkIds.size() * linkIds.size();
		int current = 0;

		for (int i = 0; i < linkIds.size(); i++) {
			for (int j = 0; j < linkIds.size(); j++) {
				current++;
				System.out.println(String.format("%d/%d", current, total));

				Link fromLink = network.getLinks().get(linkIds.get(i));
				Link toLink = network.getLinks().get(linkIds.get(j));

				Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), 0.0, null, null);

				double distance = path.links.stream().mapToDouble(Link::getLength).sum();
				String sequence = path.links.stream().map(Link::getId).map(Id::toString)
						.collect(Collectors.joining(","));

				outputWriter.write(String.join(";", new String[] { //
						linkIds.get(i).toString(), linkIds.get(j).toString(), //
						String.valueOf(distance), sequence //
				}) + "\n");
			}
		}

		outputWriter.close();
	}
}
