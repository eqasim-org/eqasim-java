package org.eqasim.ile_de_france.policies.city_tax;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.costs.IDFCarCostModel;
import org.eqasim.ile_de_france.policies.city_tax.model.CityTaxModel;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.population.routes.NetworkRoute;

public class CityTaxCostModel implements CostModel {
	private final IDFCarCostModel delegate;
	private final CityTaxModel taxModel;

	public CityTaxCostModel(IDFCarCostModel delegate, CityTaxModel taxModel) {
		this.delegate = delegate;
		this.taxModel = taxModel;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double tax_EUR = 0.0;

		for (PlanElement element : elements) {
			if (element instanceof Leg leg) {
				if (leg.getRoute() instanceof NetworkRoute route) {
					tax_EUR += taxModel.getMonetaryCost(route);
				}
			}
		}

		return delegate.calculateCost_MU(person, trip, elements) + tax_EUR;
	}
}
