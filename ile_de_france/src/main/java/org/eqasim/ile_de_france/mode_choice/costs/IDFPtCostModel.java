package org.eqasim.ile_de_france.mode_choice.costs;

import java.util.List;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFSpatialPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFSpatialVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

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

	private double calculateRailDistance_km(List<? extends PlanElement> elements) {
		double railDistance_km = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals(TransportMode.pt)) {
					EnrichedTransitRoute route = (EnrichedTransitRoute) leg.getRoute();

					String transportMode = transitSchedule.getTransitLines().get(route.getTransitLineId()).getRoutes()
							.get(route.getTransitRouteId()).getTransportMode();

					if (transportMode.equals("rail")) {
						railDistance_km += leg.getRoute().getDistance() * 1e-3;
					}
				}
			}
		}

		return railDistance_km;
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

		if (isWithinParis) {
			return 1.8;
		}

		// III) Otherwise, calculate the cost of the RER, but at least 1.80 EUR per
		// trip.

		double railDistance_km = calculateRailDistance_km(elements);
		return Math.max(1.8, railDistance_km * 0.15);
	}
}
