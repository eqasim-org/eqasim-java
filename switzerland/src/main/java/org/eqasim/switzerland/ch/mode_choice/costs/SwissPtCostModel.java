package org.eqasim.switzerland.ch.mode_choice.costs;

import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.ch.mode_choice.costs.pt.PtStageCostCalculator;
import org.eqasim.switzerland.ch.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPersonVariables;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch.mode_choice.utilities.variables.SwissPtVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

public class SwissPtCostModel extends AbstractCostModel {
	private final SwissCostParameters parameters;
	private final SwissPersonPredictor predictor;
	private final SwissPtRoutePredictor ptRoutePredictor;
	private final SwissPtStageCostCalculator calculators;

	@Inject
	public SwissPtCostModel(SwissCostParameters costParameters, SwissPersonPredictor predictor, SwissPtRoutePredictor ptRoutePredictor, SwissPtStageCostCalculator calculators) {
		super("pt");

		this.parameters       = costParameters;
		this.predictor        = predictor;
		this.ptRoutePredictor = ptRoutePredictor;
		this.calculators      = calculators;
	}

	protected double calculateHomeDistance_km(SwissPersonVariables variables, DiscreteModeChoiceTrip trip) {
		double originHomeDistance_km = CoordUtils.calcEuclideanDistance(variables.homeLocation,
				trip.getOriginActivity().getCoord()) * 1e-3;
		double destinationHomeDistance_km = CoordUtils.calcEuclideanDistance(variables.homeLocation,
				trip.getDestinationActivity().getCoord()) * 1e-3;
		return Math.max(originHomeDistance_km, destinationHomeDistance_km);
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SwissPersonVariables personVariables = predictor.predictVariables(person, trip, elements);
		SwissPtVariables ptVariables         = ptRoutePredictor.predictVariables(person, trip, elements);
		double price                         = 0;
		double legPrice                      = 0;
		double totalDistance                 = 0;

		if (personVariables.hasGeneralSubscription || personVariables.age_a < 6) {
			return 0.0;
		}

		boolean halfFareTariff = personVariables.hasHalbtaxSubscription || (personVariables.age_a <= 16);

		// TODO find a better way to identify which regional subscription the agent has access to
		if (personVariables.hasRegionalSubscription) {
			double homeDistance_km = calculateHomeDistance_km(personVariables, trip);

			if (homeDistance_km <= parameters.ptRegionalRadius_km) {
				return 0.0;
			}
		}
		
		Map<String, List<SwissPtLegVariables>> groupedByAuthority =  ptVariables.getPricingStrategy();

		//System.out.println("\nStarting to compute the price for a trip from " + trip.getOriginActivity().getCoord().toString() + " to " + trip.getDestinationActivity().getCoord().toString());

		for (Map.Entry<String, List<SwissPtLegVariables>> entry : groupedByAuthority.entrySet()){
            String authority                        = entry.getKey();
            List<SwissPtLegVariables> authorityLegs = entry.getValue();
			PtStageCostCalculator calculator        = this.calculators.priceCalculators.get("None");

			if (this.calculators.priceCalculators.containsKey(authority)){
				calculator = this.calculators.priceCalculators.get(authority);				
			}

			legPrice = calculator.calculatePrice(authorityLegs, halfFareTariff, authority);
			price += legPrice;
			//System.out.println("  Computed price for authority " + authority + ": " + legPrice);

			for (SwissPtLegVariables leg : authorityLegs){
				totalDistance += leg.networkDistance / 1000.0;
			}
        }

		double oldPriceModel = 0.6 * totalDistance;
		if (halfFareTariff){
			oldPriceModel /= 2;
		}
		oldPriceModel = Math.round(oldPriceModel * 100.0) / 100.0;

        //System.out.println("\nTotal price: " + price);
		//System.out.println("Old cost model (0.6*distance): " + oldPriceModel + "\n");

		return price;
	}
}
