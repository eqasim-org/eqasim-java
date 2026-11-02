package org.sutlab.seville_extended.mode_choice.utilities.estimators;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.sutlab.seville_extended.mode_choice.parameters.SevilleModeParameters;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevillePersonPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevillePtPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.predictors.SevilleTripPredictor;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevillePersonVariables;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevillePtVariables;
import org.sutlab.seville_extended.mode_choice.utilities.variables.SevilleTripVariables;

import java.util.List;

public class SevillePtUtilityEstimator implements UtilityEstimator {
	private final SevilleModeParameters parameters;
	private final SevillePersonPredictor personPredictor;
	private final SevillePtPredictor ptPredictor;
	private final SevilleTripPredictor tripPredictor;
	private final CostModel costModel;

	@Inject
	public SevillePtUtilityEstimator(SevilleModeParameters parameters, SevillePtPredictor ptPredictor,
									 SevillePersonPredictor personPredictor, SevilleTripPredictor tripPredictor,
									 @Named("pt") CostModel costModel) {
		this.personPredictor = personPredictor;
		this.ptPredictor = ptPredictor;
		this.tripPredictor = tripPredictor;
		this.parameters = parameters;
		this.costModel = costModel;
	}

	protected double estimateConstantUtility() {
		return parameters.pt.alpha_u;
	}

	protected double estimateAccessEgressTimeUtility(SevillePtVariables variables) {
		return parameters.betaAccessTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateLineSwitchUtility(SevillePtVariables variables) {
		return parameters.pt.betaLineSwitch_u * variables.numberOfLineSwitches;
	}

	protected double estimateWaitingTimeUtility(SevillePtVariables variables) {
		return parameters.pt.betaWaitingTime_u_min * variables.waitingTime_min;
	}

	protected double estimateMonetaryCostUtility(double cost_EUR) {
		return parameters.betaCost_u_MU * cost_EUR;
	}

	protected double estimateInVehicleTimeUtility(SevillePtVariables variables) {
		return parameters.pt.betaInVehicleTime_u_min * variables.inVehicleTime_min;
	}

	protected double estimateDrivingPermitUtility(SevillePersonVariables variables) {
		return variables.hasDrivingPermit ? parameters.sevillePt.betaDrivingPermit_u : 0.0;
	}

	protected double estimateOnlyBus(SevillePtVariables variables) {
		return variables.isOnlyBus ? parameters.sevillePt.onlyBus_u : 0.0;
	}

	protected double estimateOutsideBoundaryUtility(SevilleTripVariables variables) {
		return variables.isOutsideBoundary ? parameters.sevillePt.betaOutsideBoundary_u : 0.0;
	}

	protected double estimateDistanceUtility(SevilleTripVariables variables) {
		return parameters.sevillePt.betaDistanceAbove0_u_km * variables.distanceAbove0_km
				+ parameters.sevillePt.betaDistanceAbove2_u_km * variables.distanceAbove2_km
				+ parameters.sevillePt.betaDistanceAbove5_u_km * variables.distanceAbove5_km
				+ parameters.sevillePt.betaDistanceAbove10_u_km * variables.distanceAbove10_km;
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SevillePersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);
		SevillePtVariables ptVariables = ptPredictor.predictVariables(person, trip, elements);
		SevilleTripVariables tripVariables = tripPredictor.predictVariables(person, trip, elements);

		double cost_EUR = costModel.calculateCost_MU(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(ptVariables);
		utility += estimateLineSwitchUtility(ptVariables);
		utility += estimateWaitingTimeUtility(ptVariables);
		utility += estimateMonetaryCostUtility(cost_EUR);
		utility += estimateInVehicleTimeUtility(ptVariables);

		utility += estimateOnlyBus(ptVariables);
		utility += estimateDrivingPermitUtility(personVariables);
		utility += estimateOutsideBoundaryUtility(tripVariables);
		utility += estimateDistanceUtility(tripVariables);

		return utility;
	}
}
