package org.eqasim.ile_de_france.mode_choice.costs;

import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.eqasim.ile_de_france.routing.IDFRaptorUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

/**
 * Added PT subsidy for agents working too far away
 * 
 * @author akramelb
 */

public class IDFPtCostModel implements CostModel {
	private final IDFPersonPredictor personPredictor;
	private final IDFSpatialPredictor spatialPredictor;
	private final Map<Person, Double> homeWorkDistances;

	// TODO: This should be hidden by some custom predictor
	private final TransitSchedule transitSchedule;

	@Inject
	public IDFPtCostModel(IDFPersonPredictor personPredictor, IDFSpatialPredictor spatialPredictor,
			TransitSchedule transitSchedule, Map<Person, Double> homeWorkDistances) {
		this.personPredictor = personPredictor;
		this.spatialPredictor = spatialPredictor;
		this.transitSchedule = transitSchedule;
		this.homeWorkDistances = homeWorkDistances;
	}

	private boolean isOnlyMetroOrBus(List<? extends PlanElement> elements) {
		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().startsWith(IDFRaptorUtils.PT_MODE_PREFIX)) {
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
	private final static double regressionA = 0.098;
	private final static double regressionB = 0.006;
	private final static double regressionC = 0.006;
	private final static double regressionD = -0.77;

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// 0) If agent works too far away, PT cost is subsidized
		double homeWorkDistance_km = homeWorkDistances.get(person);
		if (homeWorkDistance_km > 3000) {
			return 0;
		}

		// I) If the person has a subscription, the price is zero!

		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		if (personVariables.hasSubscription) {
			return 0.0;
		}

		// II) Special case: Within Paris or only metro and bus
		IDFSpatialVariables spatialVariables = spatialPredictor.predictVariables(person, trip, elements);
		boolean isWithinParis = spatialVariables.hasUrbanOrigin && spatialVariables.hasUrbanDestination;

		boolean isOnlyMetroOrBus = isOnlyMetroOrBus(elements);

		if (isOnlyMetroOrBus || isWithinParis) {
			return 1.8;
		}

		// III) Otherwise, use regression by Azise Diallo
		double directDistance_km = 1e-3 * CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
				trip.getDestinationActivity().getCoord());

		double originCenterDistance_km = 1e-3
				* CoordUtils.calcEuclideanDistance(CENTER, trip.getOriginActivity().getCoord());

		double destinationCenterDistance_km = 1e-3
				* CoordUtils.calcEuclideanDistance(CENTER, trip.getDestinationActivity().getCoord());

		return Math.max(1.9, sigmoid(regressionA * directDistance_km + regressionB * originCenterDistance_km
				+ regressionC * destinationCenterDistance_km + regressionD));
	}

	private double sigmoid(double x) {
		return 1.0 / (1.0 + Math.exp(x));
	}
}
