package org.eqasim.examples.zurich_adpt.mode_choice.costs;

import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.cost.AbstractCostModel;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.CordonChargingData;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zone;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import com.google.inject.Inject;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class CordonPricingCarCostModel extends AbstractCostModel {
	private final SwissCostParameters parameters;
	private final CordonChargingData cordonCharingData;
	@Inject
	public CordonPricingCarCostModel(SwissCostParameters costParameters, CordonChargingData cordonChargingData) {
		super("car");
		this.parameters = costParameters;
		this.cordonCharingData = cordonChargingData;
	}

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		
		Coord startCoord = trip.getOriginActivity().getCoord();
		Coord endCoord = trip.getDestinationActivity().getCoord();
		double departureTime = trip.getDepartureTime();
		int departureHour = (int) (departureTime /3600.0);
		boolean foundStart = false;
		boolean foundEnd = false;

		Map<String, Zone> mapZones = this.cordonCharingData.getMapCordon();

		for (Zone zone : mapZones.values()) {

			if (zone.containsCoordinate(startCoord)) {
				foundStart = true;
			}
			if (zone.containsCoordinate(endCoord)) {
				foundEnd = true;
			}			
		}
		double cordonCharge = 0.0;
		if (!foundStart && foundEnd) {
			if (this.cordonCharingData.getMapCordCharges().get(departureHour) == null) {
				throw new IllegalStateException(
						String.format("There is an agent departing at the hour that is not in the charging data file: ",
								departureHour + "h time is missing."));
				
			}
			cordonCharge = this.cordonCharingData.getMapCordCharges().get(departureHour);
		}
		return cordonCharge + parameters.carCost_CHF_km * getInVehicleDistance_km(elements);
	}
}
