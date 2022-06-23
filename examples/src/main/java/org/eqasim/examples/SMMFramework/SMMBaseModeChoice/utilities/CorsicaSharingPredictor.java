package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.utilities;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.List;

public class CorsicaSharingPredictor extends CachedVariablePredictor<CorsicaSharingVariables> {
	private CostModel costModel;
	@Inject
	public CorsicaSharingPredictor(@Named("sharing:bikeShare") CostModel costModel) {
		this.costModel = costModel;
	}
	@Override
	public CorsicaSharingVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		double sharedBikeSpeed=6.11;// Proxy of 22kph
		double travelTime_min = 0.0;
		double accessTime_min = 0.0;
		double egressTime_min=0.0;
		double detour_min=0.0;
		double cost_MU = 0.0;
		double parkingTime_min = 1;// Proxy
		PlanElement lastElement=null;
		for (PlanElement tempElement:elements){

			if(tempElement instanceof  Leg){
				Leg tempLeg= (Leg) tempElement;
				if(lastElement!=null&& tempLeg.getMode()==TransportMode.walk){
					if (lastElement instanceof Activity){
						Activity lastActivity=(Activity)lastElement;
						if(lastActivity.getType().contentEquals("sharing booking interaction")){
							accessTime_min =+ tempLeg.getTravelTime().seconds()/60;
						}
						if(lastActivity.getType().contentEquals("sharing dropoff interaction")){
							egressTime_min=+tempLeg.getTravelTime().seconds()/60;
						}

					}
				}
				System.out.println(tempLeg.getMode());
				if(lastElement!=null&& tempLeg.getMode().contentEquals(TransportMode.bike)){
					travelTime_min=+tempLeg.getRoute().getTravelTime().seconds()/60;

				}

			}
		}

		double euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);
		double beelineTravelTime=euclideanDistance_km*1000/sharedBikeSpeed;
		double routedTravelTime=accessTime_min+egressTime_min+travelTime_min;
		cost_MU=costModel.calculateCost_MU(person,trip,elements);
		if(beelineTravelTime<routedTravelTime){
			detour_min=beelineTravelTime-routedTravelTime;
		}
		return new CorsicaSharingVariables(travelTime_min,cost_MU,euclideanDistance_km,accessTime_min,egressTime_min,detour_min,parkingTime_min);
	}
}
