package org.eqasim.ile_de_france.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

public class IDFPtCostModel implements CostModel {
	private final IDFPersonPredictor personPredictor;
	private final IDFSpatialPredictor spatialPredictor;

	// TODO: This should be hidden by some custom predictor
	private final TransitSchedule transitSchedule;

	@Inject
	public IDFPtCostModel(IDFPersonPredictor personPredictor, IDFSpatialPredictor spatialPredictor,
			TransitSchedule transitSchedule) {
		this.personPredictor = personPredictor;
		this.spatialPredictor = spatialPredictor;
		this.transitSchedule = transitSchedule;
	}

	private boolean isOnlyMetroOrBus(List<? extends PlanElement> elements) {
		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals(TransportMode.pt)) {
					TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

					String transportMode = transitSchedule.getTransitLines().get(route.getLineId()).getRoutes()
							.get(route.getRouteId()).getTransportMode();

					if (!transportMode.equals("bus") && !transportMode.equals("subway")) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private final static Coord CENTER = new Coord(651726, 6862287);

	private double calculateBasisDistance_km(DiscreteModeChoiceTrip trip) {
		return 1e-3 * (CoordUtils.calcEuclideanDistance(CENTER, trip.getOriginActivity().getCoord())
				+ CoordUtils.calcEuclideanDistance(CENTER, trip.getDestinationActivity().getCoord()));
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// I) If the person has a subscription, the price is zero!

		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		if (personVariables.hasSubscription) {
			return 0.0;
		}

		// II) If the trip is entirely inside of Paris, or it only consists of metro and
		// bus, the price is 1.80 EUR

		IDFSpatialVariables spatialVariables = spatialPredictor.predictVariables(person, trip, elements);
		boolean isWithinParis = spatialVariables.hasUrbanOrigin && spatialVariables.hasUrbanDestination;

		boolean isOnlyMetroOrBus = isOnlyMetroOrBus(elements);

		if (isOnlyMetroOrBus || isWithinParis) {
			return 1.8;
		}

		/*- 
		 * III) Otherwise, we calculate as follows:
		 *
		 * 1) Determine the Euclidean distance from the origin station to the center of Paris.
		 * 2) Determine the Euclidean distance from the destination station to the center of Paris.
		 * 3) Add up the two distances to arrive at the distance D as the basis for price calculation.
		 * 4) Calculate 0.25 EUR/km * D to arrive at a rough price estimate.
		 * 
		 * This assumes that trips in Île-de-France usually must cross through Paris (and otherwise
		 * they would usually be a bus). And some brief experimentation with the route planner of 
		 * RATP showed that the prices are roghly constructed by the total ride distance with
		 * a price per distance. TODO: A more detailed analysis would be good to have!
		 */

		return 0.25 * calculateBasisDistance_km(trip);
	}
}
