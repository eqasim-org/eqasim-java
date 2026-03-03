package org.eqasim.switzerland.ch_cmdp.mode_choice.costs;

import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.PtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.SwissPtStageCostCalculator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPtRoutePredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtVariables;
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
		double price                         = 0;

		boolean isGleis7 = trip.getDepartureTime() < 5 * 3600 || trip.getDepartureTime() >= 19 * 3600;
		boolean hasGleis7FreeTravel = personVariables.age_a < 25 && personVariables.hasGleis7Subscription && isGleis7;
		boolean hasFreePublicTransport = personVariables.hasGeneralSubscription
										|| personVariables.age_a < 6
										|| (personVariables.age_a < 16 && personVariables.hasJuniorSubscription)
										|| hasGleis7FreeTravel;
		if (hasFreePublicTransport) {
			return 0.0;
		}

		// for testing purposes, we give ga to 70% of long distances trips
//		double euclideanDistance_km = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord()) * 1e-3;
//		if (euclideanDistance_km>20.0) {
//			double randomValue = Math.random();
//			if (randomValue > 0.5) {
//				return 0.0;
//			}
//		}

		// TODO find a better way to identify which regional subscription the agent has access to
		if (personVariables.hasRegionalSubscription) {
			double homeDistance_km = calculateHomeDistance_km(personVariables, trip);
			if (homeDistance_km <= parameters.ptRegionalRadius_km) {
				return 0.0;
			}
		}

		SwissPtVariables ptVariables         = ptRoutePredictor.predictVariables(person, trip, elements);
		Map<String, List<SwissPtLegVariables>> groupedByAuthority =  ptVariables.getPricingStrategy();

		boolean halfFareTariff = personVariables.hasHalbtaxSubscription || (personVariables.age_a <= 16);
		double legPrice                      = 0;
		double totalDistance                 = 0;
		for (Map.Entry<String, List<SwissPtLegVariables>> entry : groupedByAuthority.entrySet()){
            String authority                        = entry.getKey();
            List<SwissPtLegVariables> authorityLegs = entry.getValue();
			PtStageCostCalculator calculator        = this.calculators.priceCalculators.get("None");

			if (this.calculators.priceCalculators.containsKey(authority)){
				calculator = this.calculators.priceCalculators.get(authority);				
			}

			legPrice = calculator.calculatePrice(authorityLegs, halfFareTariff, authority);
			price += legPrice;

			for (SwissPtLegVariables leg : authorityLegs){
				totalDistance += leg.networkDistance / 1000.0;
			}
        }

		double maximumPrice = halfFareTariff? 35.0 : 60.0;
		return Math.min(price, maximumPrice);
	}
}
