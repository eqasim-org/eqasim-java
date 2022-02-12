package org.eqasim.ile_de_france.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFCostParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;

import com.google.inject.Inject;

public class NantesPtCostModel implements CostModel {
	private final IDFPersonPredictor personPredictor;
	private final IDFCostParameters parameters;

	@Inject
	public NantesPtCostModel(IDFPersonPredictor personPredictor, IDFCostParameters parameters) {
		this.personPredictor = personPredictor;
		this.parameters = parameters;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// I) If the person has a subscription, the price is zero!

		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		if (personVariables.hasSubscription) {
			return 0.0;
		}

		// II) Otherwise we use a price per hour

		double firstDepartureTime = Double.NaN;
		double lastArrivalTime = Double.NaN;

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().equals(TransportMode.pt)) {
					TransitPassengerRoute route = (TransitPassengerRoute) leg.getRoute();

					if (Double.isNaN(firstDepartureTime)) {
						firstDepartureTime = route.getBoardingTime().seconds();
					}

					lastArrivalTime = leg.getDepartureTime().seconds() + leg.getTravelTime().seconds();
				}
			}
		}

		if (Double.isNaN(firstDepartureTime) || Double.isNaN(lastArrivalTime)) {
			return 0.0; // Not a real public transport trip
		}

		double hours = Math.ceil((lastArrivalTime - firstDepartureTime) / 3600.0);
		return hours * parameters.ptCost_EUR_h;
	}
}
