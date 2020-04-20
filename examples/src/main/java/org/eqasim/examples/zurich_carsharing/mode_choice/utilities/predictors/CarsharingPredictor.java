package org.eqasim.examples.zurich_carsharing.mode_choice.utilities.predictors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.examples.zurich_carsharing.listeners.CarsharingAvailabilityListener;
import org.eqasim.examples.zurich_carsharing.mode_choice.costs.CarsharingCostModel;
import org.eqasim.examples.zurich_carsharing.mode_choice.utilities.variables.CarsharingVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.FreeFloatingVehiclesContainer;
import org.matsim.contrib.carsharing.qsim.FreefloatingAreas;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class CarsharingPredictor extends CachedVariablePredictor<CarsharingVariables> {
	private CarsharingAvailabilityListener carsharingListener;
	private CarsharingSupplyInterface supply;
	private Network networkFF;
	private LeastCostPathCalculator pathCalculator;
	private CarsharingCostModel costModel;

	@Inject
	public CarsharingPredictor(CarsharingAvailabilityListener carsharingListener, CarsharingSupplyInterface supply,
			@Named("ff") TravelTime travelTimes, Map<String, TravelDisutilityFactory> travelDisutilityFactories,
			@Named("carnetwork") Network networkFF, LeastCostPathCalculatorFactory pathCalculatorFactory,
			CarsharingCostModel costModel) {
		this.networkFF = networkFF;
		this.carsharingListener = carsharingListener;
		this.supply = supply;
		TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car)
				.createTravelDisutility(travelTimes);
		this.pathCalculator = pathCalculatorFactory.createPathCalculator(networkFF, travelDisutility, travelTimes);
		this.costModel = costModel;
	}

	@Override
	public CarsharingVariables predict(Person person, DiscreteModeChoiceTrip trip,
			List<? extends PlanElement> elements) {

		double euclideanDistance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
				trip.getDestinationActivity().getCoord());
		Map<String, ArrayList<QuadTree<CSVehicle>>> avail = this.carsharingListener.getAvailability();

		int departureTimeIndex = (int) (trip.getDepartureTime() / 900.0);
		if (departureTimeIndex > 119)
			departureTimeIndex = 119;
		for (String company : avail.keySet()) {
			FreefloatingAreas areas = ((FreeFloatingVehiclesContainer) supply.getCompany(company)
					.getVehicleContainer("freefloating")).getFreefloatingAreas();
			if (!(areas.contains(trip.getOriginActivity().getCoord())
					&& areas.contains(trip.getDestinationActivity().getCoord())))
				continue;

			QuadTree<CSVehicle> qt = avail.get(company).get(departureTimeIndex);
			QuadTree<Double> rentals = this.carsharingListener.getRentals().get(departureTimeIndex);

			CSVehicle veh = qt.getClosest(trip.getOriginActivity().getCoord().getX(),
					trip.getOriginActivity().getCoord().getY());
			if (veh == null)
				continue;
			Link locationVeh = this.carsharingListener.getLocations().get(company).get(departureTimeIndex).get(veh);
			if (locationVeh == null)
				throw new RuntimeException("System is in the inconsistent state!");

			int numRentals = rentals.getDisk(trip.getOriginActivity().getCoord().getX(),
					trip.getOriginActivity().getCoord().getY(), 500.0).size();
			int numVeh = qt.getDisk(trip.getOriginActivity().getCoord().getX(),
					trip.getOriginActivity().getCoord().getY(), 500.0).size();
			if (numRentals >= numVeh) {
				if (!(MatsimRandom.getRandom().nextDouble() < numVeh / (numRentals + 1)))
					continue;
			}
			Link destinationLink = this.networkFF.getLinks().get(trip.getDestinationActivity().getLinkId());
			Path path = pathCalculator.calcLeastCostPath(locationVeh.getToNode(), destinationLink.getFromNode(),
					trip.getDepartureTime(), person, null);

			double accessTime = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
					locationVeh.getCoord()) * 1.3 / 1.1;

			double cost = this.costModel.calculateCost(path.travelTime);
			return new CarsharingVariables(path.travelTime / 60.0, cost, euclideanDistance, accessTime / 60.0, true);
		}
		return new CarsharingVariables(1.0, 1.0, 1.0, 1.0, false);

	}
}
