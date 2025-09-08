package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import org.eqasim.core.components.calibration.writer.VariablesWriter;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.PtUtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PtPredictor;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissModeParameters;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SwissPtUtilityEstimator extends PtUtilityEstimator {
    private final SwissModeParameters parameters;
    private final PtPredictor predictor;

    @Inject
    public SwissPtUtilityEstimator(SwissModeParameters parameters, PtPredictor predictor) {
        super(parameters, predictor);
        this.predictor  = predictor;
        this.parameters = parameters;
    }

    protected double estimateCantonUtility(Person person) {
        Object cantonObj = person.getAttributes().getAttribute("cantonName");
        if (cantonObj instanceof String canton) {
            return parameters.swissCanton.pt.getOrDefault(canton, 0.0);
        }
        return 0.0;
    }

    @Override
    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        double utility = 0.0;

        utility += super.estimateUtility(person, trip, elements);
        utility += estimateCantonUtility(person);

        if(VariablesWriter.isInitiated()) {
            PtVariables variables = predictor.predictVariables(person, trip, elements);
            writeVariablesToCsv(person, trip, variables, utility);
        }
        return utility;
    }

    private void writeVariablesToCsv(Person person, DiscreteModeChoiceTrip trip, PtVariables variables, double utility) {
        double departureTime = trip.getDepartureTime();
        int tripIndex = trip.getIndex();
        String personId = person.getId().toString();

        Map<String, String> ptAttributes = new HashMap<>();
        ptAttributes.put("accessEgressTime_min", String.valueOf(variables.accessEgressTime_min));
        ptAttributes.put("inVehicleTime_min", String.valueOf(variables.inVehicleTime_min));
        ptAttributes.put("waitingTime_min", String.valueOf(variables.waitingTime_min));
        ptAttributes.put("numberOfLineSwitches", String.valueOf(variables.numberOfLineSwitches));
        ptAttributes.put("cost_MU", String.valueOf(variables.cost_MU));
        ptAttributes.put("euclideanDistance_km", String.valueOf(variables.euclideanDistance_km));

        VariablesWriter.writeVariables("pt", personId, tripIndex, departureTime, utility, ptAttributes);
    }

}