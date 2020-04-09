package org.eqasim.examples.zurich_adpt.mode_choice.utilities.predictors;

import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.ZonalVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.variables.AdPTVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zone;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zones;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;

public class AdPTPredictor extends CachedVariablePredictor<AdPTVariables> {
	private LeastCostPathCalculator pathCalculator;
	private Zones zones;
	private ZonalVariables zonalCosts;
	@Inject
	public AdPTPredictor(@Named("car") TravelTime travelTimes,
			Map<String, TravelDisutilityFactory> travelDisutilityFactories, @Named("carnetwork") Network networkFF,
			LeastCostPathCalculatorFactory pathCalculatorFactory, Zones zones, ZonalVariables zonalCosts,
			Scenario scenario) {
		TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car)
				.createTravelDisutility(travelTimes);
		this.pathCalculator = pathCalculatorFactory.createPathCalculator(networkFF, travelDisutility, travelTimes);
		this.zones = zones;
		this.zonalCosts = zonalCosts;
	}

	@Override
	protected AdPTVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {

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
		
		double cost = this.zonalCosts.getZoneZoneCosts().get(startZoneId).get(endZoneId);
		Coordinate coordinateStopStart = mapZones.get(startZoneId).geometry.getCentroid().getCoordinate();
		Coord coordStopStart = CoordUtils.createCoord(coordinateStopStart);
		
		Coordinate coordinateStopEnd = mapZones.get(endZoneId).geometry.getCentroid().getCoordinate();
		Coord coordStopEnd = CoordUtils.createCoord(coordinateStopEnd);
		
		
		//calculate access/egress times
		
		
		

		return null;
	}

}
