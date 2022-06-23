package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.cost;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;

import org.eqasim.examples.SMMFramework.SMMBaseModeChoice.parameters.CorsicaSharingCostParameters;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class CorsicaSharingCostModel extends AbstractCostModel {
	private final CorsicaSharingCostParameters parameters;
	@Inject
	public CorsicaSharingCostModel(CorsicaSharingCostParameters parameters) {
		super("sharing:bikeShare");
		this.parameters = parameters;
	}
	@Override
	protected double getInVehicleDistance_km(List<? extends PlanElement> elements){
		double inVehicleDistance=0.0;
		for (PlanElement element :elements){
			if(element instanceof Leg){
				element=(Leg)element;
				if(((Leg) element).getMode()== TransportMode.bike){
					inVehicleDistance=+((Leg) element).getRoute().getDistance();
				}
			}

		}
		return (inVehicleDistance);
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double tripDistance_km = getInVehicleDistance_km(elements);
		return parameters.sharingCost_Eur_Km * tripDistance_km+parameters.sharingBooking_Cost;
	}
}
