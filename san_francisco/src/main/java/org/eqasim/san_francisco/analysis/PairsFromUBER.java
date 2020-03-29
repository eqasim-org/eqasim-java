package org.eqasim.san_francisco.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class PairsFromUBER {

	public static void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();
		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));
		writer.write(String.join(",", new String[] { //
				"start_coord_x", "start_coord_y", "id_start", "end_coord_x", "end_coord_y", "id_end",
				"departure_time" }) + "\n");
		writer.flush();

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(cmd.getOptionStrict("input-path"))));

		reader.readLine();
		String s = reader.readLine();
		List<Data> listOfCentroids_o = new ArrayList<>();
		List<Data> listOfCentroids_d = new ArrayList<>();
		while (s != null) {
			String[] arr = s.split(",");
			Data d = new Data();
			d.coord_x = Double.parseDouble(arr[0]);
			d.coord_y = Double.parseDouble(arr[1]);
			d.id = Integer.parseInt(arr[2]);
			listOfCentroids_o.add(d);
			listOfCentroids_d.add(d);
			s = reader.readLine();
		}
		reader.close();
		int i = 0;
		for (Data data_o : listOfCentroids_o) {
			for (Data data_d : listOfCentroids_d) {
				if (data_o.id != data_d.id) {
					writer.write(i++ + "," + data_o.coord_x + "," + data_o.coord_y + "," + data_o.id + "," + data_d.coord_x + ","
							+ data_d.coord_y + "," + data_d.id + "," + String.valueOf(5.0 * 3600.0) + "\n");
					writer.write(i++ + "," + data_o.coord_x + "," + data_o.coord_y + "," + data_o.id + "," + data_d.coord_x + ","
							+ data_d.coord_y + "," + data_d.id + "," + String.valueOf(7.0 * 3600.0) + "\n");
					writer.write(i++ + "," + data_o.coord_x + "," + data_o.coord_y + "," + data_o.id + "," + data_d.coord_x + ","
							+ data_d.coord_y + "," + data_d.id + "," + String.valueOf(12.0 * 3600.0) + "\n");
					writer.write(i++ + "," + data_o.coord_x + "," + data_o.coord_y + "," + data_o.id + "," + data_d.coord_x + ","
							+ data_d.coord_y + "," + data_d.id + "," + String.valueOf(18.0 * 3600.0) + "\n");
					writer.flush();
				}
			}
		}
		writer.close();

	}

}
