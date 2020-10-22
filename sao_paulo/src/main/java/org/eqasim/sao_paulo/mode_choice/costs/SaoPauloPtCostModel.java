package org.eqasim.sao_paulo.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.sao_paulo.mode_choice.parameters.SaoPauloCostParameters;
import org.eqasim.sao_paulo.mode_choice.utilities.predictors.SaoPauloPersonPredictor;
import org.eqasim.sao_paulo.mode_choice.utilities.variables.SaoPauloPersonVariables;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import com.google.inject.Inject;

public class SaoPauloPtCostModel implements CostModel {
	private final SaoPauloPersonPredictor predictor;
	private final SaoPauloCostParameters parameters;
	private final Scenario scenario;

	@Inject
	public SaoPauloPtCostModel(SaoPauloCostParameters parameters, SaoPauloPersonPredictor predictor,
			Scenario scenario) {
		this.predictor = predictor;
		this.parameters = parameters;
		this.scenario = scenario;
	}

	public int getNumberOfMetroRailVehicles(List<? extends PlanElement> elements) {
		int n_Vehicles = 0;
		String mode = "pt";

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().contentEquals(mode)) {

					TransitLine tl = scenario.getTransitSchedule().getTransitLines()
							.get(((TransitPassengerRoute) leg.getRoute()).getLineId());
					TransitRoute tr = tl.getRoutes().get(((TransitPassengerRoute) leg.getRoute()).getRouteId());
					if (tr.getTransportMode().equals("subway") || tr.getTransportMode().equals("rail"))
						n_Vehicles += 1;
				}
			}
		}
		return n_Vehicles;
	}

	public int getNumberOfBusVehicles(List<? extends PlanElement> elements) {
		int n_Vehicles = 0;
		String mode = "pt";

		for (PlanElement element : elements) {
			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				if (leg.getMode().contentEquals(mode)) {

					TransitLine tl = scenario.getTransitSchedule().getTransitLines()
							.get(((TransitPassengerRoute) leg.getRoute()).getLineId());
					TransitRoute tr = tl.getRoutes().get(((TransitPassengerRoute) leg.getRoute()).getRouteId());
					if (tr.getTransportMode().equals("bus"))
						n_Vehicles += 1;
				}
			}
		}
		return n_Vehicles;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		SaoPauloPersonVariables variables = predictor.predictVariables(person, trip, elements);

		if (variables.hasSubscription) {
			return 0.0;
		}

		int n_VehiclesMR = getNumberOfMetroRailVehicles(elements);
		int n_VehiclesBus = getNumberOfBusVehicles(elements);

		if (n_VehiclesBus == 0 || n_VehiclesMR == 0)
			return parameters.ptCostPerTrip_0Transfers_BRL;
		else if (n_VehiclesBus > 0 && n_VehiclesMR > 0)
			return parameters.ptCostPerTrip_3Transfers_BRL;
		else
			return parameters.ptCostPerTrip_0Transfers_BRL;

	}
}
