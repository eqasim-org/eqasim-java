
package org.eqasim.jakarta.eventhandling;



import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.charts.XYLineChart;
/**
 * This EventHandler implementation counts the 
 * traffic volume on the link with id number 6 and
 * provides a method to write the hourly volumes
 * to a chart png.
 * @author dgrether
 *
 */
public class MyEventHandler1 implements LinkEnterEventHandler {

	private double[] volumeLink100680;


	public MyEventHandler1() {
		reset(0);
	}

	public double getTravelTime(int slot) {
		return this.volumeLink100680[slot];
	}
	
	private int getSlot(double time){
		return (int)time/3600;
	}

	@Override
	public void reset(int iteration) {
		this.volumeLink100680 = new double[24];
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (event.getLinkId().equals(Id.create("100680", Link.class))) {
			this.volumeLink100680[getSlot(event.getTime())]++;
		}	
	}


	public void writeChart(String filename) {
		double[] hours = new double[24];
		for (double i = 0.0; i < 24.0; i++){
			hours[(int)i] = i;
		}
		XYLineChart chart = new XYLineChart("Traffic link 100680", "hour", "departures");
		chart.addSeries("times", hours, this.volumeLink100680);
		chart.saveAsPng(filename, 800, 600);
	}

}