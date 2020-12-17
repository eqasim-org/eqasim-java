package org.eqasim.ile_de_france;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class RunAnalysis {
	static public void main(String[] args) throws IOException {
		String eventsPath = "simulation_output/output_events.xml.gz";
		String outputPath = "flows.csv";

		FlowCollector collector = new FlowCollector();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(collector);

		eventsManager.initProcessing();
		new MatsimEventsReader(eventsManager).readFile(eventsPath);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputPath))));

		writer.write("link_id;normal_count;hub_count\n");

		for (Map.Entry<Id<Link>, Flow> item : collector.flow.entrySet()) {
			writer.write(
					String.format("%s;%d;%d\n", item.getKey().toString(), item.getValue().normal, item.getValue().hub));
		}

		writer.close();
	}

	static class Flow {
		Id<Link> linkId;
		long normal = 0;
		long hub = 0;

		public Flow(Id<Link> linkId) {
			this.linkId = linkId;
		}
	}

	static class FlowCollector implements LinkEnterEventHandler {
		IdMap<Link, Flow> flow = new IdMap<>(Link.class);

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getVehicleId().toString().startsWith("freight_")) {
				String[] parts = event.getVehicleId().toString().split("_");
				boolean isHub = parts[1].equals("hub");

				flow.computeIfAbsent(event.getLinkId(), k -> new Flow(k));

				Flow local = flow.get(event.getLinkId());

				if (isHub) {
					local.hub += 1;
				} else {
					local.normal += 1;
				}
			}
		}
	}
}
