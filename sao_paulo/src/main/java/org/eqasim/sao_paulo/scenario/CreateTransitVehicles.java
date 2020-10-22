package org.eqasim.sao_paulo.scenario;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class CreateTransitVehicles {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		TransitScheduleReader tsr = new TransitScheduleReader(scenario);
		tsr.readFile(args[0]);
		Set<Id<Vehicle>> vehicleids = new HashSet<>();
		for (TransitLine tl : scenario.getTransitSchedule().getTransitLines().values()) {

			for (TransitRoute tr : tl.getRoutes().values()) {
				for (Departure dp : tr.getDepartures().values()) {
					if (vehicleids.contains(dp.getVehicleId())) {
						Id<Vehicle> newId = Id.createVehicleId(dp.getVehicleId().toString() + "_1");
						vehicleids.add(newId);
						dp.setVehicleId(newId);
					} else
						vehicleids.add(dp.getVehicleId());
				}
			}
		}
		VehicleType busVehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("Bus", VehicleType.class));
		scenario.getTransitVehicles().addVehicleType(busVehicleType);
		VehicleType subwayVehicleType = VehicleUtils.getFactory()
				.createVehicleType(Id.create("Subway", VehicleType.class));
		scenario.getTransitVehicles().addVehicleType(subwayVehicleType);
		VehicleType railVehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("Rail", VehicleType.class));
		scenario.getTransitVehicles().addVehicleType(railVehicleType);

		for (Id<Vehicle> id : vehicleids) {

			Vehicles vehicles = scenario.getTransitVehicles();
			if (id.toString().contains("bus")) {
				Vehicle v = vehicles.getFactory().createVehicle(id, busVehicleType);
				scenario.getTransitVehicles().addVehicle(v);

			}
			if (id.toString().contains("subway")) {
				Vehicle v = vehicles.getFactory().createVehicle(id, subwayVehicleType);
				scenario.getTransitVehicles().addVehicle(v);

			}
			if (id.toString().contains("rail")) {
				Vehicle v = vehicles.getFactory().createVehicle(id, railVehicleType);
				scenario.getTransitVehicles().addVehicle(v);

			}
		}

		MatsimVehicleWriter vw = new MatsimVehicleWriter(scenario.getTransitVehicles());
		vw.writeFile(args[1]);

		TransitScheduleWriter tsw = new TransitScheduleWriter(scenario.getTransitSchedule());
		tsw.writeFile(args[2]);
	}

}
