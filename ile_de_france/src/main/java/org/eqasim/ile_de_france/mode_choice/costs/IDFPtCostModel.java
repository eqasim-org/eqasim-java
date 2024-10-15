package org.eqasim.ile_de_france.mode_choice.costs;

import java.util.List;

import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPersonPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPtPredictor;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPersonVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.IDFPtVariables;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

public class IDFPtCostModel implements CostModel {
	private final IDFPersonPredictor personPredictor;
	private final IDFPtPredictor ptPredictor;

	@Inject
	public IDFPtCostModel(IDFPersonPredictor personPredictor, IDFPtPredictor ptPredictor) {
		this.personPredictor = personPredictor;
		this.ptPredictor = ptPredictor;
	}

	private final static Coord CENTER = new Coord(651726, 6862287);

	private final static double regressionA = 0.098;
	private final static double regressionB = 0.006;
	private final static double regressionC = 0.006;
	private final static double regressionD = -0.77;
	private final static double basePrice = 5.5;

	@Override
	public double calculateCost_MU(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		// I) If the person has a subscription, the price is zero!
		IDFPersonVariables personVariables = personPredictor.predictVariables(person, trip, elements);

		if (personVariables.hasSubscription) {
			return 0.0;
		}

		// II) Special case: Within Paris or only metro and bus
		IDFPtVariables ptVariables = ptPredictor.predictVariables(person, trip, elements);

		if (ptVariables.hasOnlySubwayAndBus || ptVariables.isWithinParis) {
			return 1.9;
		}

		// III) Otherwise, use regression by Abdelkader DIB
		double directDistance_km = 1e-3 * CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
				trip.getDestinationActivity().getCoord());

		double originCenterDistance_km = 1e-3
				* CoordUtils.calcEuclideanDistance(CENTER, trip.getOriginActivity().getCoord());

		double destinationCenterDistance_km = 1e-3
				* CoordUtils.calcEuclideanDistance(CENTER, trip.getDestinationActivity().getCoord());

		return Math.max(1.9, basePrice * sigmoid(regressionA * directDistance_km + regressionB * originCenterDistance_km
				+ regressionC * destinationCenterDistance_km + regressionD));
	}

	private double sigmoid(double x) {
		return 1.0 / (1.0 + Math.exp(x));
	}
}
