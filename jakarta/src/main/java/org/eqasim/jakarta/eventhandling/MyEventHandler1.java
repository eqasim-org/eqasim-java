package org.eqasim.jakarta.eventhandling;



import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
/**
 * This EventHandler implementation counts the 
 * traffic volume on the link with id number 6 and
 * provides a method to write the hourly volumes
 * to a chart png.
 * @author dgrether
 *
 */
public class MyEventHandler1 implements LinkEnterEventHandler {

	private Map<String, int[]> volumeLink100680;
	private Map<String, int[]> volumeLink199420;
	private Map<String, int[]> volumeLink1120574;
	private Map<String, int[]> volumeLink63745;
	private Map<String, int[]> volumeLink611155;
	private Map<String, int[]> volumeLink618832;
	private Map<String, int[]> volumeLink561700;


	public MyEventHandler1() {
		reset(0);
	}

	
	
	private int getSlot(double time){
		return (int)time/3600;
	}

	@Override
	public void reset(int iteration) {
		writeChart(volumeLink100680, iteration + "_100680.csv" );
		this.volumeLink100680 = new HashMap<>();
		writeChart(volumeLink199420, iteration + "_199420.csv" );
		this.volumeLink199420 = new HashMap<>();
		writeChart(volumeLink1120574, iteration + "_1120574.csv" );
		this.volumeLink1120574 = new HashMap<>();
		writeChart(volumeLink63745, iteration + "_63745.csv" );
		this.volumeLink63745 = new HashMap<>();
		writeChart(volumeLink611155, iteration + "_611155.csv" );
		this.volumeLink611155 = new HashMap<>();
		writeChart(volumeLink618832, iteration + "_618832.csv" );
		this.volumeLink618832 = new HashMap<>();
		writeChart(volumeLink561700, iteration + "_561700.csv" );
		this.volumeLink561700 = new HashMap<>();
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		String vehicle_id = event.getVehicleId().toString();
		String[]vehicleid = vehicle_id.split("_");
		String mode = "";
		if (vehicleid.length == 1)
			mode = "car";
		else
		    mode = vehicleid[1];
		
		int []vol = null;
		if (event.getLinkId().equals(Id.create("100680", Link.class))) {
		 vol = this.volumeLink100680.get(mode);
		if(vol == null) {
			vol = new int [24];
			this.volumeLink100680.put(mode, vol);
		}
		}
		if (event.getLinkId().equals(Id.create("199420", Link.class))) {
			 vol = this.volumeLink199420.get(mode);
			if(vol == null) {
				vol = new int [24];
				this.volumeLink199420.put(mode, vol);
			}
			}
		if (event.getLinkId().equals(Id.create("1120574", Link.class))) {
			 vol = this.volumeLink1120574.get(mode);
			if(vol == null) {
				vol = new int [24];
				this.volumeLink1120574.put(mode, vol);
			}
			}
		if (event.getLinkId().equals(Id.create("63745", Link.class))) {
			 vol = this.volumeLink63745.get(mode);
			if(vol == null) {
				vol = new int [24];
				this.volumeLink63745.put(mode, vol);
			}
			}
		if (event.getLinkId().equals(Id.create("611155", Link.class))) {
			 vol = this.volumeLink611155.get(mode);
			if(vol == null) {
				vol = new int [24];
				this.volumeLink611155.put(mode, vol);
			}
			}
		if (event.getLinkId().equals(Id.create("618832", Link.class))) {
			 vol = this.volumeLink618832.get(mode);
			if(vol == null) {
				vol = new int [24];
				this.volumeLink618832.put(mode, vol);
			}
			}
		if (event.getLinkId().equals(Id.create("561700", Link.class))) {
			 vol = this.volumeLink561700.get(mode);
			if(vol == null) {
				vol = new int [24];
				this.volumeLink561700.put(mode, vol);
			}
			}
		
		if (vol != null) {
			vol[getSlot(event.getTime())]++;
		}
	
	}


	public void writeChart(Map<String, int[]> vol, String filename) {
		if (vol == null)return;
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
        try {
			writer.write("mode, time, vol\n");
        for (Entry<String, int[]> e:vol.entrySet()) {
        	for (int hour = 0; hour < e.getValue().length; hour++) {
        		writer.write(e.getKey() + "," + hour +"," +  e.getValue()[hour] + "\n");
        	}
        }
        writer.flush();
        writer.close();
        
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
		
	}
}