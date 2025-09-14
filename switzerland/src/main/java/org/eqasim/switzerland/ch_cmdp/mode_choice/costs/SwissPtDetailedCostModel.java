package org.eqasim.switzerland.ch_cmdp.mode_choice.costs;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCostParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPersonPredictor;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPersonVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.List;


public class SwissPtDetailedCostModel extends AbstractCostModel {
    private final SwissCostParameters parameters;
    private final SwissPersonPredictor predictor;

    @Inject
    public SwissPtDetailedCostModel(SwissCostParameters costParameters, SwissPersonPredictor predictor) {
        super("pt");

        this.parameters = costParameters;
        this.predictor = predictor;
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
        SwissPersonVariables variables = predictor.predictVariables(person, trip, elements);

        boolean isGleis7 = trip.getDepartureTime() < 5 * 3600 || trip.getDepartureTime() > 19 * 3600;
        boolean hasGleis7FreeTravel = variables.age_a < 25 && variables.hasGleis7Subscription && isGleis7;

        boolean hasFreePublicTransport = variables.hasGeneralSubscription
                || variables.age_a < 6
                || (variables.age_a < 16 && variables.hasJuniorSubscription)
                || hasGleis7FreeTravel;

        if (hasFreePublicTransport) {
            return 0.0;
        }

        if (variables.hasRegionalSubscription) {
            double homeDistance_km = calculateHomeDistance_km(variables, trip);
            if (homeDistance_km <= parameters.ptRegionalRadius_km) {
                return 0.0;
            }
        }

        double inVehicleDistance = getInVehicleDistance_km(elements);
        double fullCost_CHF = Math.max(parameters.ptMinimumCost_CHF,
                parameters.ptCost_CHF_km * inVehicleDistance + parameters.ptCost_CHF_km2 * Math.pow(inVehicleDistance,2));

        if (variables.hasHalbtaxSubscription) {
            return Math.min(fullCost_CHF * 0.5 , 50.0);
        }

        if (variables.age_a < 16) {
            return Math.min(fullCost_CHF * 0.5 , 50.0);
        }

        return Math.min(fullCost_CHF, 50.0);
    }
}
