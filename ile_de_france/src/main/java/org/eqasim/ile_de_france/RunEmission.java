package org.eqasim.ile_de_france;


import org.eqasim.ile_de_france.scenario.OsmHbefaMapping;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

public class RunEmission {
	

	static public void main(String[] args) {
		// Create config group for emissions
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();

		// Set up emissions (use files from the emissions examples on github.com)
		ecg.setWritingEmissionsEvents(true);
		ecg.setAverageWarmEmissionFactorsFile("Lille_EFA_HOT_vehcat_2010average.txt");
		ecg.setAverageColdEmissionFactorsFile("Lille_EFA_ColdStart_vehcat_2010average.txt");
		ecg.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		ecg.setNonScenarioVehicles(NonScenarioVehicles.ignore);

		// Load Île-de-France config file and add emissions group
//		Config config = ConfigUtils.loadConfig("C:\\Users\\Liu Liu\\MoMo\\Sce_Eg\\LVMT_ETZH\\idf_1pm\\ile_de_france_config.xml");
		Config config = ConfigUtils.loadConfig("C:\\Users\\Liu Liu\\MoMo\\Sce_Eg\\LVMT_ETZH\\idf_10pct\\network_modified_10pct_idf\\10pct_networkmodification_cut_saint-denis\\network_modified_10pct_sd\\output_config.xml");
//		config.network().setInputFile("output_network.xml.gz");
		config.addModule(ecg);

		// Load scenario and set up events manager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		// We have to set the "hbefa_road_type" attribute of all links -> here we always set the same value
//		for (Link link : scenario.getNetwork().getLinks().values()) {
//			link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
		//}
		
		OsmHbefaMapping abc = OsmHbefaMapping.build();
		Network network = scenario.getNetwork();
		abc.addHbefaMappings(network);
		new NetworkWriter(network).write("sd_hbefa_network.xml.gz");
		
//		System.exit(0);

		// We need to set up vehicles
		Vehicles vehicles = scenario.getVehicles();

		// ... we need to create a vehicle type
		VehicleType vehicleType = vehicles.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		vehicles.addVehicleType(vehicleType);

		vehicleType.setCapacity(new VehicleCapacityImpl());
		vehicleType.setDescription("BEGIN_EMISSIONSPASSENGER_CAR;average;average;averageEND_EMISSIONS");

		// ... we need to give every person a vehicle
		for (Person person : scenario.getPopulation().getPersons().values()) {
			// Vehicles need to have the same ID as the persons they belong to
			Id<Person> personId = person.getId(); 
			Id<Vehicle> vehicleId = Id.createVehicleId(personId);

			Vehicle vehicle = vehicles.getFactory().createVehicle(vehicleId, vehicleType);
			vehicles.addVehicle(vehicle);
		}

		// This needs to be set so MATSim actually uses the vehicles that we create above
		config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		// From here everything is as in the offline emissions contrib example

		// This prepares the emissions module
		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};;

		com.google.inject.Injector injector = Injector.createInjector(config, module );

		// Here we get the emissions module
		EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

		// Here we define where we want to write the new events file with emission events
		EventWriterXML emissionEventWriter = new EventWriterXML( "C:\\Users\\Liu Liu\\MoMo\\Sce_Eg\\LVMT_ETZH\\idf_10pct\\network_modified_10pct_idf\\10pct_networkmodification_cut_saint-denis\\network_modified_10pct_sd\\emissions.xml.gz" );
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		// Here we use the events reader to read in old events
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile("C:\\Users\\Liu Liu\\MoMo\\Sce_Eg\\LVMT_ETZH\\idf_10pct\\network_modified_10pct_idf\\10pct_networkmodification_cut_saint-denis\\network_modified_10pct_sd\\ITERS\\it.40\\40.events.xml.gz");

		emissionEventWriter.closeFile();
	}
}
