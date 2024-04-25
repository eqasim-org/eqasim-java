package org.matsim.contribs.discrete_mode_choice.model.tour_based;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.eqasim.core.simulation.mode_choice.constraints.EqasimVehicleTourConstraint;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.components.estimators.CumulativeTourEstimator;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.TourFinder;
import org.matsim.contribs.discrete_mode_choice.components.utils.LocationUtils;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.CompositeTourConstraint;
import org.matsim.contribs.discrete_mode_choice.model.constraints.TourFromTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGenerator;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGeneratorFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilitySelectorFactory;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;

import java.util.*;
import java.util.stream.Collectors;

public class EfficientTourBasedModel implements DiscreteModeChoiceModel {
    final private static Logger logger = LogManager.getLogger(EfficientTourBasedModel.class);

    final public static boolean COMPARE_AGAINST_OLD_METHOD = false;

    final private TourFinder tourFinder;
    final private TourFilter tourFilter;
    final private TourEstimator estimator;
    final private ModeAvailability modeAvailability;
    final private TourConstraintFactory constraintFactory;
    final private UtilitySelectorFactory selectorFactory;
    final private ModeChainGeneratorFactory modeChainGeneratorFactory;
    final private FallbackBehaviour fallbackBehaviour;
    final private TimeInterpretation timeInterpretation;

    public EfficientTourBasedModel(TourEstimator estimator, ModeAvailability modeAvailability,
                                           TourConstraintFactory constraintFactory, TourFinder tourFinder, TourFilter tourFilter,
                                           UtilitySelectorFactory selectorFactory, ModeChainGeneratorFactory modeChainGeneratorFactory,
                                           FallbackBehaviour fallbackBehaviour, TimeInterpretation timeInterpretation) {
        this.estimator = estimator;
        this.modeAvailability = modeAvailability;
        this.constraintFactory = constraintFactory;
        this.tourFinder = tourFinder;
        this.tourFilter = tourFilter;
        this.selectorFactory = selectorFactory;
        this.modeChainGeneratorFactory = modeChainGeneratorFactory;
        this.fallbackBehaviour = fallbackBehaviour;
        this.timeInterpretation = timeInterpretation;
    }

    @Override
    public List<TripCandidate> chooseModes(Person person, List<DiscreteModeChoiceTrip> trips, Random random) throws NoFeasibleChoiceException {
        List<String> modes = new ArrayList<>(modeAvailability.getAvailableModes(person, trips));
        TourConstraint constraint = constraintFactory.createConstraint(person, trips, modes);

        List<TourCandidate> tourCandidates = new LinkedList<>();
        List<List<String>> tourCandidateModes = new LinkedList<>();

        int tripIndex = 1;
        TimeTracker timeTracker = new TimeTracker(timeInterpretation);

        for (List<DiscreteModeChoiceTrip> tourTrips : tourFinder.findTours(trips)) {
            timeTracker.addActivity(tourTrips.get(0).getOriginActivity());

            // We pass the departure time through the first origin activity
            tourTrips.get(0).setDepartureTime(timeTracker.getTime().seconds());

            TourCandidate finalTourCandidate = null;

            if (tourFilter.filter(person, tourTrips)) {
                CompositeTourConstraint compositeTourConstraint = (CompositeTourConstraint) constraint;
                CumulativeTourEstimator cumulativeTourEstimator = (CumulativeTourEstimator) estimator;
                ModeChoiceModelTree modeChoiceModelTree = new ModeChoiceModelTree(person, tourTrips, compositeTourConstraint, cumulativeTourEstimator.getDelegate(), modes, tourCandidates, timeInterpretation);
                modeChoiceModelTree.build();
                UtilitySelector selector = selectorFactory.createUtilitySelector();
                List<TourCandidate> oldCandidates = null;
                if(COMPARE_AGAINST_OLD_METHOD) {
                    oldCandidates = this.oldMethod(tourTrips, modes, person, constraint, tourCandidateModes);
                }
                List<TourCandidate> efficientCandidates = modeChoiceModelTree.getTourCandidates();
                if(COMPARE_AGAINST_OLD_METHOD && efficientCandidates.size() != oldCandidates.size()) {
                    throw new IllegalStateException(String.format("Problem with person %s at trip %d", person.getId(), tripIndex));
                }
                for(TourCandidate tourCandidate: modeChoiceModelTree.getTourCandidates()) {
                    selector.addCandidate(tourCandidate);
                }
                Optional<UtilityCandidate> selectedCandidate = selector.select(random);

                if (!selectedCandidate.isPresent()) {
                    switch (fallbackBehaviour) {
                        case INITIAL_CHOICE:
                            logger.warn(
                                    buildFallbackMessage(tripIndex, person, "Setting tour modes back to initial choice."));
                            selectedCandidate = Optional.of(createFallbackCandidate(person, tourTrips, tourCandidates));
                            break;
                        case IGNORE_AGENT:
                            return handleIgnoreAgent(tripIndex, person, tourTrips);
                        case EXCEPTION:
                            throw new NoFeasibleChoiceException(buildFallbackMessage(tripIndex, person, ""));
                    }
                }

                finalTourCandidate = (TourCandidate) selectedCandidate.get();
            } else {
                finalTourCandidate = createFallbackCandidate(person, tourTrips, tourCandidates);
            }

            tourCandidates.add(finalTourCandidate);
            tourCandidateModes.add(
                    finalTourCandidate.getTripCandidates().stream().map(c -> c.getMode()).collect(Collectors.toList()));

            tripIndex += tourTrips.size();

            for (int i = 0; i < tourTrips.size(); i++) {
                if (i > 0) { // Our time object is already at the end of the first activity
                    timeTracker.addActivity(tourTrips.get(i).getOriginActivity());
                }

                timeTracker.addDuration(finalTourCandidate.getTripCandidates().get(i).getDuration());
            }
        }

        return createTripCandidates(tourCandidates);
    }

    private TourCandidate createFallbackCandidate(Person person, List<DiscreteModeChoiceTrip> tourTrips,
                                                  List<TourCandidate> tourCandidates) {
        List<String> initialModes = tourTrips.stream().map(DiscreteModeChoiceTrip::getInitialMode)
                .collect(Collectors.toList());
        return estimator.estimateTour(person, initialModes, tourTrips, tourCandidates);
    }

    private List<TripCandidate> createTripCandidates(List<TourCandidate> tourCandidates) {
        return tourCandidates.stream().map(TourCandidate::getTripCandidates).flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<TripCandidate> handleIgnoreAgent(int tripIndex, Person person, List<DiscreteModeChoiceTrip> trips) {
        List<TourCandidate> tourCandidates = new LinkedList<>();

        for (List<DiscreteModeChoiceTrip> tourTrips : tourFinder.findTours(trips)) {
            List<String> tourModes = tourTrips.stream().map(DiscreteModeChoiceTrip::getInitialMode)
                    .collect(Collectors.toList());
            tourCandidates.add(estimator.estimateTour(person, tourModes, tourTrips, tourCandidates));
        }

        logger.warn(buildFallbackMessage(tripIndex, person, "Setting whole plan back to initial modes."));
        return createTripCandidates(tourCandidates);
    }

    private String buildFallbackMessage(int tripIndex, Person person, String appendix) {
        return String.format("No feasible mode choice candidate for tour starting at trip %d of agent %s. %s",
                tripIndex, person.getId().toString(), appendix);
    }

    private String buildIllegalUtilityMessage(int tripIndex, Person person, TourCandidate candidate) {
        TripCandidate trip = candidate.getTripCandidates().get(tripIndex);

        return String.format(
                "Received illegal utility for for tour starting at trip %d (%s) of agent %s. Continuing with next candidate.",
                tripIndex, trip.getMode(), person.getId().toString());
    }

    public List<TourCandidate> oldMethod(List<DiscreteModeChoiceTrip> tourTrips, List<String> modes, Person person, TourConstraint constraint, List<List<String>> tourCandidateModes) {
        ModeChainGenerator generator = modeChainGeneratorFactory.createModeChainGenerator(modes, person,
                tourTrips);
        List<TourCandidate> tourCandidates = new ArrayList<>();

        while (generator.hasNext()) {
            List<String> tourModes = generator.next();

            if (!constraint.validateBeforeEstimation(tourTrips, tourModes, tourCandidateModes)) {
                continue;
            }

            TourCandidate candidate = estimator.estimateTour(person, tourModes, tourTrips, tourCandidates);

            if (!Double.isFinite(candidate.getUtility())) {
                continue;
            }

            if (!constraint.validateAfterEstimation(tourTrips, candidate, tourCandidates)) {
                continue;
            }

            tourCandidates.add(candidate);
        }
        return tourCandidates;
    }

    public static class ModeChoiceModelTree {
        public List<DiscreteModeChoiceTrip> getTourTrips() {
            return tourTrips;
        }

        public List<TourConstraint> getTourConstraints() {
            return tourConstraints;
        }

        public List<TripConstraint> getTripConstraints() {
            return tripConstraints;
        }

        public TripEstimator getTripEstimator() {
            return tripEstimator;
        }

        public Collection<String> getModes() {
            return modes;
        }

        private final List<DiscreteModeChoiceTrip> tourTrips;
        private final List<TourConstraint> tourConstraints;
        private final List<TripConstraint> tripConstraints;
        private final TripEstimator tripEstimator;
        private final Collection<String> modes;
        private final List<TourCandidate> previousTourCandidates;
        private final Person person;
        private final TimeInterpretation timeInterpretation;
        private ModeChoiceModelTreeNode root;
        private final Set<String> restrictedModes = new HashSet<>();
        private Id<? extends BasicLocation> vehicleLocationId;

        public List<TourCandidate> getPreviousTourCandidates() {
            return previousTourCandidates;
        }

        public Person getPerson() {
            return person;
        }

        public TimeInterpretation getTimeInterpretation() {
            return this.timeInterpretation;
        }

        public ModeChoiceModelTree(Person person, List<DiscreteModeChoiceTrip> tourTrips, CompositeTourConstraint tourConstraint, TripEstimator tripEstimator, Collection<String> modes, List<TourCandidate> previousTourCandidates, TimeInterpretation timeInterpretation) {
            this.person = person;
            this.tourTrips = tourTrips;
            this.previousTourCandidates = previousTourCandidates;
            this.tourConstraints = new ArrayList<>();
            this.tripEstimator = tripEstimator;
            this.modes = modes;
            this.tripConstraints = new ArrayList<>();
            for(TourConstraint innerTourConstraint: tourConstraint.getConstraints()) {
                if(innerTourConstraint instanceof TourFromTripConstraint tourFromTripConstraint) {
                    this.tripConstraints.add(tourFromTripConstraint.getConstraint());
                } else {
                    this.tourConstraints.add(innerTourConstraint);
                }
            }
            this.timeInterpretation = timeInterpretation;
        }

        public void build() {
            TimeTracker timeTracker = new TimeTracker(timeInterpretation);
            timeTracker.setTime(this.tourTrips.get(0).getDepartureTime());
            this.root = new ModeChoiceModelTreeNode(this, this.previousTourCandidates.stream().flatMap(tourCandidate -> tourCandidate.getTripCandidates().stream()).toList(), new ArrayList<>(), this.tourTrips, timeTracker, this.modes, 0);
            root.expand();
        }

        public List<TourCandidate> getTourCandidates() {
            return this.root.getTourCandidates();
        }
    }
    public static class ModeChoiceModelTreeNode {
        private final List<TripCandidate> allPreviousTrips;
        private final List<TripCandidate> currentTourPreviousTrips;
        private final List<DiscreteModeChoiceTrip> remainingTrips;
        private final TimeTracker currentTimeTracker;
        private final Collection<String> modes;
        private final double currentUtility;
        private final ModeChoiceModelTree tree;
        private final Collection<ModeChoiceModelTreeNode> children;
        private TourCandidate tourCandidate;
        public ModeChoiceModelTreeNode(ModeChoiceModelTree tree, List<TripCandidate> allPreviousTrips, List<TripCandidate> currentTourPreviousTrips, List<DiscreteModeChoiceTrip> remainingTrips, TimeTracker currentTimeTracker, Collection<String> modes, double currentUtility) {
            this.allPreviousTrips = allPreviousTrips;
            this.currentTourPreviousTrips = currentTourPreviousTrips;
            this.remainingTrips = remainingTrips;
            this.currentUtility = currentUtility;
            this.currentTimeTracker = currentTimeTracker;
            this.modes = modes;
            this.tree = tree;
            this.children = new ArrayList<>();
            this.tourCandidate = null;
        }

        public boolean expand() {
            this.children.clear();
            if(this.remainingTrips.size() == 0) {
                return true;
            }
            DiscreteModeChoiceTrip currentTrip = this.remainingTrips.get(0);
            this.currentTimeTracker.addActivity(currentTrip.getOriginActivity());
            currentTrip.setDepartureTime(currentTimeTracker.getTime().seconds());
            List<String> previousModes = this.allPreviousTrips.stream().map(TripCandidate::getMode).toList();
            for(String mode: modes)  {
                if(this.tree.tripConstraints.stream().anyMatch(tripConstraint -> !tripConstraint.validateBeforeEstimation(currentTrip, mode, previousModes))) {
                    continue;
                }
                TripCandidate tripCandidate = this.tree.getTripEstimator().estimateTrip(this.tree.getPerson(), mode, currentTrip, this.allPreviousTrips);
                TimeTracker timeTracker = new TimeTracker(this.tree.getTimeInterpretation());
                timeTracker.setTime(currentTimeTracker.getTime().seconds());
                timeTracker.addDuration(tripCandidate.getDuration());
                double utility = currentUtility + tripCandidate.getUtility();
                if(this.tree.tripConstraints.stream().anyMatch(tripConstraint -> !tripConstraint.validateAfterEstimation(currentTrip, tripCandidate, allPreviousTrips))) {
                    continue;
                }
                List<TripCandidate> allPreviousTrips = new ArrayList<>(this.allPreviousTrips);
                allPreviousTrips.add(tripCandidate);
                List<TripCandidate> currentTourPreviousTrips = new ArrayList<>(this.currentTourPreviousTrips);
                currentTourPreviousTrips.add(tripCandidate);
                List<String> newPreviousModes = currentTourPreviousTrips.stream().map(TripCandidate::getMode).toList();
                List<DiscreteModeChoiceTrip> remainingTrips = new ArrayList<>(this.remainingTrips);
                remainingTrips.remove(0);
                ModeChoiceModelTreeNode child = new ModeChoiceModelTreeNode(this.tree, allPreviousTrips, currentTourPreviousTrips, remainingTrips, timeTracker, this.modes, utility);
                if(remainingTrips.size() == 0) {
                    child.tourCandidate = new DefaultTourCandidate(utility, currentTourPreviousTrips);
                    if(this.tree.tourConstraints.stream().anyMatch(tourConstraint ->
                    {
                        if(!tourConstraint.validateBeforeEstimation(tree.tourTrips, newPreviousModes, this.tree.previousTourCandidates.stream().map(tourCandidate -> tourCandidate.getTripCandidates().stream().map(TripCandidate::getMode).toList()).toList())) {
                            return true;
                        }
                        if(!tourConstraint.validateAfterEstimation(tree.tourTrips, child.tourCandidate, this.tree.previousTourCandidates)) {
                            return true;
                        }
                        return false;
                    })) {
                        continue;
                    }
                }
                if(child.expand()) {
                    this.children.add(child);
                }
            }
            return children.size() > 0;
        }

        public List<TourCandidate> getTourCandidates() {
            List<TourCandidate> tourCandidates = new ArrayList<>();
            getTourCandidates(tourCandidates);
            return tourCandidates;
        }
        private void getTourCandidates(List<TourCandidate> candidatesList) {
            if(this.tourCandidate == null) {
                this.children.forEach(child -> child.getTourCandidates(candidatesList));
            } else {
                candidatesList.add(this.tourCandidate);
            }
        }
    }

}
