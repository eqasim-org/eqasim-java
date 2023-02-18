package org.matsim.contribs.discrete_mode_choice.components.estimators;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.replanning.time_interpreter.TimeInterpreter;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

/**
 * This is an abstract estimator class that makes it easy to rely on MATSim's
 * TripRouter. Instead of just getting a proposed mode, this class already
 * routes the trip with the given mode in the background. All that remains is to
 * analyze the PlanElements to estimate a utility.
 * 
 * @author sebhoerl
 */
public abstract class AbstractTripRouterEstimator implements TripEstimator {
	private final TripRouter tripRouter;
	private final ActivityFacilities facilities;
	private final TimeInterpreter.Factory timeInterpreterFactory;
	private final PreroutingLogic preroutingLogic;

	public AbstractTripRouterEstimator(TripRouter tripRouter, ActivityFacilities facilities,
			TimeInterpreter.Factory timeInterpreterFactory, PreroutingLogic preroutingLogic) {
		this.tripRouter = tripRouter;
		this.facilities = facilities;
		this.timeInterpreterFactory = timeInterpreterFactory;
		this.preroutingLogic = preroutingLogic;
	}

	private boolean allowPrerouting(String mode, Person person, DiscreteModeChoiceTrip trip) {
		if (mode.equals(trip.getInitialMode())) {
			return preroutingLogic.keepInitialRoute(person, trip);
		}

		return false;
	}

	@Override
	public final TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips) {
		// I) Find the correct origin and destination facilities

		Facility originFacility = FacilitiesUtils.toFacility(trip.getOriginActivity(), facilities);
		Facility destinationFacility = FacilitiesUtils.toFacility(trip.getDestinationActivity(), facilities);

		boolean sameMode = trip.getInitialMode().equals(mode);

		if (sameMode && preroutingLogic.keepInitialRoute(person, trip)) {
			// If we already have the route of interest, just pass it on
			return estimateTripCandidate(person, mode, trip, previousTrips, trip.getInitialElements());
		} else {
			// II) Perform the routing
			List<? extends PlanElement> elements = tripRouter.calcRoute(mode, originFacility, destinationFacility,
					trip.getDepartureTime(), person);

			if (sameMode && preroutingLogic.keepInitialRoute(person, trip, elements)) {
				// We explicitly don't want to update to the new route (maybe because it is
				// worse)
				return estimateTripCandidate(person, mode, trip, previousTrips, trip.getInitialElements());
			} else {
				// III) Perform utility estimation
				return estimateTripCandidate(person, mode, trip, previousTrips, elements);
			}
		}
	}

	/**
	 * Implement this if you just want to calculate a utility, but don't want to
	 * return a custom TripCandidate object.
	 */
	protected double estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> routedTrip) {
		return 0.0;
	}

	/**
	 * Implement this if you want to return a custom TripCandidate object rather
	 * than just a utility.
	 */
	protected TripCandidate estimateTripCandidate(Person person, String mode, DiscreteModeChoiceTrip trip,
			List<TripCandidate> previousTrips, List<? extends PlanElement> routedTrip) {

		TimeInterpreter time = timeInterpreterFactory.createTimeInterpreter();
		time.setTime(trip.getDepartureTime());
		time.addPlanElements(routedTrip);

		double utility = estimateTrip(person, mode, trip, previousTrips, routedTrip);

		double duration = time.getCurrentTime() - trip.getDepartureTime();
		return new DefaultRoutedTripCandidate(utility, mode, routedTrip, duration);
	}

	static public interface PreroutingLogic {
		boolean keepInitialRoute(Person person, DiscreteModeChoiceTrip trip);

		boolean keepInitialRoute(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> updatedRoute);
	}

	static public class DefaultPreroutingLogic implements PreroutingLogic {
		private final Set<String> preroutingModes;

		public DefaultPreroutingLogic(Set<String> preroutingModes) {
			this.preroutingModes = preroutingModes;
		}

		@Override
		public boolean keepInitialRoute(Person person, DiscreteModeChoiceTrip trip) {
			if (preroutingModes.contains(trip.getInitialMode())) {
				Leg initialLeg = (Leg) trip.getInitialElements().get(0);
				double initialDepartureTime = initialLeg.getDepartureTime().seconds();

				if (initialDepartureTime == trip.getDepartureTime()) {
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean keepInitialRoute(Person person, DiscreteModeChoiceTrip trip,
				List<? extends PlanElement> updatedRoute) {
			return false;
		}
	}
}
