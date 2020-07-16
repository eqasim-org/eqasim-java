package ch.ethz.matsim.discrete_mode_choice.replanning;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripRouter;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceModel;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceModel.NoFeasibleChoiceException;
import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * This replanning algorithm uses a predefined discrete mode choice model to
 * perform mode decisions for a given plan.
 * 
 * @author sebhoerl
 */
public class DiscreteModeChoiceAlgorithm implements PlanAlgorithm {
	private final Random random;
	private final DiscreteModeChoiceModel modeChoiceModel;

	public DiscreteModeChoiceAlgorithm(Random random, DiscreteModeChoiceModel modeChoiceModel) {
		this.random = random;
		this.modeChoiceModel = modeChoiceModel;
	}

	@Override
	/**
	 * Performs mode choice on a plan. We assume that TripsToLegs has been called
	 * before, hence the code is working diretly on legs.
	 */
	public void run(Plan plan) {
		// I) First build a list of DiscreteModeChoiceTrips

		List<? extends PlanElement> elements = plan.getPlanElements();
		List<DiscreteModeChoiceTrip> trips = new ArrayList<>((elements.size() - 2) / 2 + 1);
		List<Leg> legs = new ArrayList<>((elements.size() - 2) / 2 + 1);

		TripListConverter.convert(plan, trips, legs);

		// II) Run mode choice

		try {
			// Perform mode choice and retrieve candidates
			List<TripCandidate> chosenCandidates = modeChoiceModel.chooseModes(plan.getPerson(), trips, random);

			for (int i = 0; i < trips.size(); i++) {
				TripCandidate candidate = chosenCandidates.get(i);

				// Set new mode of the leg
				Leg targetLeg = legs.get(i);
				targetLeg.setMode(candidate.getMode());

				// But alternatively put the whole routed plan segment if routing has been
				// performed in the choice
				if (candidate instanceof RoutedTripCandidate) {
					DiscreteModeChoiceTrip trip = trips.get(i);
					RoutedTripCandidate routedCandidate = (RoutedTripCandidate) candidate;
					List<? extends PlanElement> routedElements = routedCandidate.getRoutedPlanElements();

					TripRouter.insertTrip(plan, trip.getOriginActivity(), routedElements,
							trip.getDestinationActivity());
				} else {
					targetLeg.setRoute(null);
				}

				trips.get(i).getOriginActivity().getAttributes().putAttribute("utility", candidate.getUtility());
			}
		} catch (NoFeasibleChoiceException e) {
			throw new IllegalStateException(e);
		}
	}
}
