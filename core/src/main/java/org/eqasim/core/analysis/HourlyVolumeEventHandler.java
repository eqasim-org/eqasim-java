package org.eqasim.core.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.charts.XYLineChart;

public class HourlyVolumeEventHandler implements LinkLeaveEventHandler{
	
	private Map<Id<Link>, double[] > hourlyCounts = new HashMap<>(); 

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Link> id = event.getLinkId();
		double time = event.getTime();
		int hour = (int) (time / 3600);
		
		// hourlyCounts maps the link ids with arrays containing the number of vehicles that left
		// the link during each day hour (MATSim day -> up to 30 hours in a day).
		
		// First check if the link is already in the dictionary, if not initialize the entry.
		if (!hourlyCounts.containsKey(id)) {
			hourlyCounts.put(id, new double[30]);			
		}
		
		// Then add one to the corresponding count
		double[] count = hourlyCounts.get(id);		
		if (hour < 30) {
			count[hour]++;
		}	
		
	}
	
	// Create ten graphs depicting the number of vehicles leaving ten random links during the day
	public void writeChart(String filename) {
		for (int i=0; i<10; i++ ) {
			List<Id<Link>> keysAsArray = new ArrayList<Id<Link>>(this.hourlyCounts.keySet());
			Random r = new Random();
			Id<Link> id = keysAsArray.get(r.nextInt(keysAsArray.size()));
			double[] counts_to_plot = hourlyCounts.get(id);
			
			double[] hours = new double[30];
			for (double j = 0.0; j < 30.0; j++){
				hours[(int)j] = j;
			}
			XYLineChart chart = new XYLineChart("Traffic link " + id.toString(), "hour", "LinkLeaveEvents");
			chart.addSeries("times", hours, counts_to_plot);
			chart.saveAsPng(filename + id.toString() + ".png", 800, 600);
		}
	}
	
	// Sum the hourly counts on all links.
	public void writeChart_AllLinks(String filename) {
		List<Id<Link>> keysAsArray = new ArrayList<Id<Link>>(this.hourlyCounts.keySet());
		double[] counts_to_plot = new double[30];
		
		for(int i=0; i<keysAsArray.size(); i++) {
			double[] counts = this.hourlyCounts.get(keysAsArray.get(i));
			
			for(int j=0; j<30; j++) {
				counts_to_plot[j] += counts[j];
			}
		}
		
		double[] hours = new double[30];
		for (double j = 0.0; j < 30.0; j++){
			hours[(int)j] = j;
		}
		XYLineChart chart = new XYLineChart("Traffic on all links", "hour", "LinkLeaveEvents");
		chart.addSeries("times", hours, counts_to_plot);
		chart.saveAsPng(filename + "all_links.png", 800, 600);
		
	}

}
