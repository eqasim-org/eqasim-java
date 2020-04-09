package org.eqasim.examples.zurich_adpt.mode_choice.utilities.predictors;

import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.AdPTCostModel;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.variables.AdPTVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zone;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zones;
import org.eqasim.examples.zurich_adpt.scenario.AdPTRoute;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AdPTPredictor extends CachedVariablePredictor<AdPTVariables> {
	private Zones zones;
	private AdPTCostModel adptCostModel;

	@Inject
	public AdPTPredictor(Zones zones, AdPTCostModel adptCostModel) {
		this.zones = zones;
		this.adptCostModel = adptCostModel;
	}

	@Override
	public AdPTVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

		Coord startCoord = trip.getOriginActivity().getCoord();

		Coord endCoord = trip.getDestinationActivity().getCoord();

		String startZoneId = null;
		String endZoneId = null;
		Map<String, Zone> mapZones = this.zones.getZones();
		boolean foundStart = false;
		boolean foundEnd = false;
		for (Zone zone : mapZones.values()) {

			if (zone.containsCoordinate(startCoord)) {
				startZoneId = zone.code;
				foundStart = true;
			}
			if (zone.containsCoordinate(endCoord)) {
				endZoneId = zone.code;
				foundEnd = true;
			}
			if (foundStart && foundEnd)
				break;
		}

		double cost = this.adptCostModel.calculateCost_MU(startZoneId, endZoneId);

		Leg leg = (Leg) elements.get(0);

		double accessTime_min = leg.getTravelTime() / 60.0;
		Activity act = (Activity) elements.get(1);
		double waitingTime_min = act.getMaximumDuration() / 60.0;

		
		leg = (Leg) elements.get(2);
		double inVehicleTime = leg.getTravelTime() / 60.0;
		AdPTRoute route = (AdPTRoute) leg.getRoute();

		leg = (Leg) elements.get(3);
		double egressTime_min = leg.getTravelTime() / 60.0;


		double euclideanDistance_km = route.getInVehicleDistance();
		AdPTVariables adptVariables = new AdPTVariables(inVehicleTime, cost, euclideanDistance_km, waitingTime_min,
				accessTime_min, egressTime_min);

		return adptVariables;
	}

}
