package org.eqasim.san_francisco.bike.network;

import org.eqasim.san_francisco.bike.reader.BikeInfo;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.*;

public class BikeNetworkFixer {

	public void addBikeLaneInfo(Network network, Map<Id<Link>, BikeInfo> bikeLaneInfo) {
		for (Link link : network.getLinks().values()) {

			// check if the highway tag is present
			if (link.getAttributes().getAsMap().containsKey("osm:way:highway")) {

				// add bike as allowed mode
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.add(TransportMode.bike);
				link.setAllowedModes(allowedModes);

				// add bike info as attributes
				Id<Link> osmId = Id.createLinkId(link.getAttributes().getAttribute("osm:way:id").toString());

				boolean isPermitted = true;
				if (link.getAttributes().getAttribute("osm:way:highway").toString().contains("motorway") ||
						link.getAttributes().getAttribute("osm:way:highway").toString().contains("trunk")) {
					isPermitted = false;
				}
				BikeInfo bikeInfo = bikeLaneInfo.getOrDefault(osmId,
						new BikeInfo(Id.createLinkId(osmId), "none", false, isPermitted));

				link.getAttributes().putAttribute("bikeFacilityClass", bikeInfo.facility);
				link.getAttributes().putAttribute("bikeOppositeDirection", bikeInfo.opposite);
				link.getAttributes().putAttribute("bikeIsPermitted", bikeInfo.permitted);
			}
		}

		int[] goldenGateBridgeIds = new int[]{ 10234, 133936, 133937, 133938, 133939, 133940, 133941, 139029,
				139030, 162165, 162166, 162170,  16613,  16614,  16615,  16616,
				182094, 186973, 196484, 196772, 196775, 196793, 196805, 196807,
				203005, 203037, 204450,  20485,  20486, 207045, 207046,  21314,
				21320, 227192, 227193, 227194, 238377, 238378, 242023, 242024,
				251293, 251294, 261218, 261754, 261961, 272681, 272682, 283373,
				283374, 299056, 299059, 299060, 299076, 303670, 303671, 306216,
				306804, 306805, 306808, 306809, 306810, 306811, 317627, 317628,
				317629, 327907, 327908,  33148,  33149, 331825, 331826, 331936,
				331937, 331938, 331939, 331940, 331941, 359333, 365634, 365635,
				365636, 365637, 365638, 365639, 372090, 381660, 381661, 381662,
				381663, 381664, 381665, 384170, 384171, 388902, 388903, 388904,
				388905, 388930, 388931, 389035, 389036,  40314, 427128, 427129,
				427234, 427235, 437487, 437488, 437489, 437490, 438279, 438280,
				443019, 443020, 443021, 443022, 443335, 443336, 446135, 446136,
				446141, 446142, 470996, 472565, 472566, 488445, 488446, 488447,
				488448, 488449, 488450,  49032, 495982, 495983, 495984, 495985,
				495986, 495987, 495992, 495993, 496355, 496358, 500251, 511637,
				511638, 519636, 519677, 519678, 525163, 525198, 525436, 525437,
				52804,  52805,  53308,  53309,  53357,  53358,  53499,  53713,
				537219, 537220, 537221, 537222, 537223, 537224, 537444, 537445,
				537446, 537447, 537448, 537449, 537451, 537452, 537453, 537454,
				539677, 539678, 539679, 539680,  53977,  54374,  54395, 549634,
				555248, 555249, 556298, 560320, 560321, 563550, 563551, 565944,
				570203, 570204, 570207, 570208, 570211, 570212, 570213, 570214,
				570215, 570216, 570217, 570218, 570228, 570229, 570230, 570231,
				572268, 572294, 574404, 574678, 575372, 575373, 575374, 575375,
				575376, 575377, 575378, 575379, 578529, 584702, 584703, 585105,
				585106, 585107, 585108, 586554, 586555, 589092, 589093, 589094,
				589095, 591439, 593153, 593154, 595794, 598144, 598145, 613711,
				616692, 616693, 618185, 618186, 621163, 621164, 621165, 621166,
				621167, 621168, 632122, 632817, 632818, 633840, 635738, 635739,
				635740, 635741, 638706, 638707, 638739, 638740, 638875, 650641,
				650642, 653977, 653978, 658717, 659800, 659801, 659826, 659827,
				659828, 659856,  70922,  74536,  74537,  74538,  74539,  81277,
				81278,  81333,  81337,  81338,  81339,  85171,  85172,  86561,
				86567,  86568,  86569,  98956,  98957,  98958,  98959,  98960,
				9992 };

		for (int i : goldenGateBridgeIds) {
			Id<Link> linkId = Id.createLinkId(i);
			network.getLinks().get(linkId).getAttributes().putAttribute("bikeIsPermitted", true);
		}
	}

	// this assumes that you already ran the above method on the network first
	public void addNewBikeLanesWithinShape(Network network, Collection<Geometry> geometries) {

		int numberTotalLinks = 0;
		int numberModifiedLinks = 0;
		double roadLengthRemoved = 0.0;
		double roadLengthInShape = 0.0;

		for (Link link : network.getLinks().values()) {

			numberTotalLinks++;

			//check if link is within geometries
			Coord coord = link.getCoord();
			if (geometries.stream().anyMatch(geometry -> geometry.contains(MGC.coord2Point(coord)))) {

				// check if the highway tag is present
				if (link.getAttributes().getAsMap().containsKey("osm:way:highway")) {

					roadLengthInShape += link.getLength() * link.getNumberOfLanes();

					// check if the link is neither a motorway or trunk
					if (!link.getAttributes().getAttribute("osm:way:highway").toString().contains("motorway") &&
							!link.getAttributes().getAttribute("osm:way:highway").toString().contains("trunk")) {

						// check if bike is permitted
						boolean bikeIsPermitted = (boolean) link.getAttributes().getAttribute("bikeIsPermitted");

						// only do something if bike is already permitted
						if (bikeIsPermitted) {

							// check the current bike facility class
							String bikeFacilityClass = link.getAttributes().getAttribute("bikeFacilityClass").toString();

							// add a bike lane if there is no facility or only a shared lane (if more than 1 lane)
							if (bikeFacilityClass.equals("none") || bikeFacilityClass.equals("class_3")) {
								double numberOfLanes = link.getNumberOfLanes();
								if (numberOfLanes >= 2.0) {

									double flowCapacity = link.getCapacity();
									double flowCapacityPerLane = flowCapacity / numberOfLanes;

									double newNumberOfLanes = numberOfLanes - 1.0;
									double newFlowCapacity = Math.round(flowCapacityPerLane * newNumberOfLanes);

									link.setNumberOfLanes(newNumberOfLanes);
									link.setCapacity(newFlowCapacity);

									roadLengthRemoved += link.getLength();

//									System.out.println(link.getId() + ", "
//											+ numberOfLanes + ", "
//											+ link.getNumberOfLanes() + ", "
//											+ flowCapacity + ", "
//											+ link.getCapacity());

									link.getAttributes().putAttribute("bikeFacilityClass", "class_2");
									numberModifiedLinks++;
								}
							}

						}

					}
				}
			}
		}

		System.out.println("Number of total links: " + numberTotalLinks);
		System.out.println("Number of modified links: " + numberModifiedLinks);
		System.out.println("Road length in shapefile : " + roadLengthInShape);
		System.out.println("Road removed (bike lanes added) : " + roadLengthRemoved);
	}

	// this assumes that you already ran the above method on the network first
	public void addNewBikeLanes(Network network) {

		int numberTotalLinks = 0;
		int numberModifiedLinks = 0;
		double roadLengthRemoved = 0.0;
		double roadLengthInShape = 0.0;

		for (Link link : network.getLinks().values()) {

			numberTotalLinks++;

			// check if the highway tag is present
			if (link.getAttributes().getAsMap().containsKey("osm:way:highway")) {

				roadLengthInShape += link.getLength() * link.getNumberOfLanes();

				// check if the link is neither a motorway or trunk
				if (!link.getAttributes().getAttribute("osm:way:highway").toString().contains("motorway") &&
						!link.getAttributes().getAttribute("osm:way:highway").toString().contains("trunk")) {

					// check if bike is permitted
					boolean bikeIsPermitted = (boolean) link.getAttributes().getAttribute("bikeIsPermitted");

					// only do something if bike is already permitted
					if (bikeIsPermitted) {

						// check the current bike facility class
						String bikeFacilityClass = link.getAttributes().getAttribute("bikeFacilityClass").toString();

						// add a bike lane if there is no facility or only a shared lane (if more than 1 lane)
						if (bikeFacilityClass.equals("none") || bikeFacilityClass.equals("class_3")) {
							double numberOfLanes = link.getNumberOfLanes();
							if (numberOfLanes >= 2.0) {

								double flowCapacity = link.getCapacity();
								double flowCapacityPerLane = flowCapacity / numberOfLanes;

								double newNumberOfLanes = numberOfLanes - 1.0;
								double newFlowCapacity = Math.round(flowCapacityPerLane * newNumberOfLanes);

								link.setNumberOfLanes(newNumberOfLanes);
								link.setCapacity(newFlowCapacity);

								roadLengthRemoved += link.getLength();

//								System.out.println(link.getId() + ", "
//										+ numberOfLanes + ", "
//										+ link.getNumberOfLanes() + ", "
//										+ flowCapacity + ", "
//										+ link.getCapacity());

								link.getAttributes().putAttribute("bikeFacilityClass", "class_2");
								numberModifiedLinks++;
							}
						}

					}

				}
			}
		}

		System.out.println("Number of total links: " + numberTotalLinks);
		System.out.println("Number of modified links: " + numberModifiedLinks);
	}

	public void createCarFreeZone(Network network, Collection<Geometry> geometries) {

		int numberModifiedLinks = 0;
		double roadLengthRemoved = 0.0;
		double capacityRemoved = 0.0;
		double bikeLengthAdded = 0.0;

		for (Link link : network.getLinks().values()) {

			//check if link is within geometries
			Coord coord = link.getCoord();
			if (geometries.stream().anyMatch(geometry -> geometry.contains(MGC.coord2Point(coord)))) {

				// check if the highway tag is present
				if (link.getAttributes().getAsMap().containsKey("osm:way:highway")) {

					numberModifiedLinks++;
					roadLengthRemoved += link.getLength() * link.getNumberOfLanes();
					capacityRemoved += link.getCapacity();

					// remove car as a mode
					Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
					allowedModes.remove(TransportMode.car);
					allowedModes.remove("car_passenger");
					allowedModes.remove("taxi");
					System.out.println(link.getId() + ", " + allowedModes.toString());
					link.setAllowedModes(allowedModes);

					link.getAttributes().putAttribute("bikeFacilityClass", "class_2");
					bikeLengthAdded += link.getLength();

				}
			}
		}

		System.out.println("Number of modified road links in geometry: " + numberModifiedLinks);
		System.out.println("Original road length in geometry : " + roadLengthRemoved);
		System.out.println("Bike length added: " + bikeLengthAdded);
		System.out.println("Capacity removed from geometry: " + capacityRemoved);
	}
}
