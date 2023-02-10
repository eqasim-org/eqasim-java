package org.eqasim.ile_de_france.mode_choice.utilities.estimators;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.mode_choice.utilities.estimators.EstimatorUtils;
import org.eqasim.core.simulation.mode_choice.utilities.variables.PtVariables;
import org.eqasim.ile_de_france.mode_choice.parameters.IDFModeParameters;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPtPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPtVariables;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

public class IDFPtUtilityEstimator implements UtilityEstimator {
	private final IDFModeParameters parameters;

	private final IDFPtPredictor ptPredictor;
	private final IDFPersonPredictor personPredictor;

	@Inject
	public IDFPtUtilityEstimator(IDFModeParameters parameters, IDFPtPredictor ptPredictor,
			IDFPersonPredictor personPredictor) {
		this.parameters = parameters;
		this.ptPredictor = ptPredictor;
		this.personPredictor = personPredictor;
	}

	protected double estimateConstantUtility() {
		return parameters.pt.alpha_u;
	}

	protected double estimateAccessEgressTimeUtility(PtVariables variables) {
		return parameters.pt.betaAccessEgressTime_u_min * variables.accessEgressTime_min;
	}

	protected double estimateInVehicleTimeUtility(PtVariables variables) {
		return parameters.pt.betaInVehicleTime_u_min * variables.inVehicleTime_min
				* EstimatorUtils.interaction(variables.euclideanDistance_km, parameters.referenceEuclideanDistance_km,
						parameters.lambdaTravelTimeEuclideanDistance);
	}

	protected double estimateHeadwayUtility(IDFPtVariables variables) {
		return parameters.idfPt.betaHeadway_u_min * variables.headway_min;
	}

	protected double estimateLineSwitchUtility(PtVariables variables) {
		return parameters.pt.betaLineSwitch_u * variables.numberOfLineSwitches;
	}

	protected double estimateMonetaryCostUtility(PtVariables variables) {
		return parameters.betaCost_u_MU * EstimatorUtils.interaction(variables.euclideanDistance_km,
				parameters.referenceEuclideanDistance_km, parameters.lambdaCostEuclideanDistance) * variables.cost_MU;
	}

	protected double estimateOnlyBusUtility(IDFPtVariables variables) {
		if (variables.isOnlyBus) {
			return parameters.idfPt.betaOnlyBus_u;
		} else {
			return 0.0;
		}
	}

	protected double estimateDrivingPermitUtility(IDFPersonVariables variables) {
		if (variables.hasDrivingPermit) {
			return parameters.idfPt.betaDrivingPermit_u;
		} else {
			return 0.0;
		}
	}

	@Override
	public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		IDFPtVariables ptVariables = ptPredictor.predictVariables(person, trip, elements);
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		double utility = 0.0;

		utility += estimateConstantUtility();
		utility += estimateAccessEgressTimeUtility(ptVariables);
		utility += estimateHeadwayUtility(ptVariables);
		utility += estimateLineSwitchUtility(ptVariables);
		utility += estimateMonetaryCostUtility(ptVariables);
		utility += estimateInVehicleTimeUtility(ptVariables);
		utility += estimateOnlyBusUtility(ptVariables);
		utility += estimateDrivingPermitUtility(personVariables);

		UtilityValues values = new UtilityValues();
		values.constant = estimateConstantUtility();
		values.accessEgressTime = estimateAccessEgressTimeUtility(ptVariables);
		values.headway = estimateHeadwayUtility(ptVariables);
		values.lineSwitch = estimateLineSwitchUtility(ptVariables);
		values.monetaryCost = estimateMonetaryCostUtility(ptVariables);
		values.inVehicleTime = estimateInVehicleTimeUtility(ptVariables);
		values.onlyBus = estimateOnlyBusUtility(ptVariables);
		values.drivingPermit = estimateDrivingPermitUtility(personVariables);
		values.total = utility;
		
		try {
			String valuesString = new ObjectMapper().writeValueAsString(values);
			trip.getOriginActivity().getAttributes().putAttribute("pt", valuesString);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		return utility;
	}
	
	static public class UtilityValues {
		public double constant;
		public double accessEgressTime;
		public double headway;
		public double lineSwitch;
		public double monetaryCost;
		public double inVehicleTime;
		public double onlyBus;
		public double drivingPermit;
		public double total;
	}
}
