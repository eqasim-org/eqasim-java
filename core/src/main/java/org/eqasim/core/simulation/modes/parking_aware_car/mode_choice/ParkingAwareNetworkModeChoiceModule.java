package org.eqasim.core.simulation.modes.parking_aware_car.mode_choice;

import com.google.common.base.Verify;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.core.simulation.modes.parking_aware_car.config.ParkingAwareNetworkModeConfigGroup;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageEventListener;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.penalty.DetailedParkingAwareCarPenaltyProvider;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.penalty.NoPenalty;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.penalty.ParkingAwareCarPenaltyProvider;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.timing.TimeInterpretation;

import java.util.List;
import java.util.Map;

public class ParkingAwareNetworkModeChoiceModule extends AbstractEqasimExtension {

    private final ParkingAwareNetworkModeConfigGroup configGroup;
    public ParkingAwareNetworkModeChoiceModule(ParkingAwareNetworkModeConfigGroup configGroup) {
        this.configGroup = configGroup;
        Verify.verify(configGroup.getParkingAwareNetworkModeChoiceConfigGroup() != null);
    }

    @Override
    protected void installEqasimExtension() {

        bindTourEstimator(ParkingAwareCumulativeTourEstimator.NAME).to(ParkingAwareCumulativeTourEstimator.class);

        ParkingAwareNetworkModeChoiceConfigGroup modeChoiceConfigGroup = this.configGroup.getParkingAwareNetworkModeChoiceConfigGroup();
        Verify.verify(modeChoiceConfigGroup != null);
        bind(DetailedParkingAwareCarPenaltyProvider.class).toProvider(new Provider<>() {

            @Inject
            private ParkingUsageEventListener parkingUsageEventListener;

            @Inject
            private NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

            @Override
            public DetailedParkingAwareCarPenaltyProvider get() {
                return new DetailedParkingAwareCarPenaltyProvider(parkingUsageEventListener, networkWideParkingSpaceStore, modeChoiceConfigGroup.getRate());
            }
        }).asEagerSingleton();

        addControlerListenerBinding().toProvider(new Provider<>() {

            @Inject
            ParkingAwareCarPenaltyProvider parkingAwareCarPenaltyProvider;

            @Override
            public ControlerListener get() {
                return (IterationEndsListener) event -> {
                    parkingAwareCarPenaltyProvider.update(event.getIteration());
                };
            }
        });

        bindUtilityEstimator("a").toProvider(new Provider<UtilityEstimator>() {
            @Inject
            ParkingAwareCarPenaltyProvider parkingAwareCarPenaltyProvider;

            @Inject
            Map<String, Provider<UtilityEstimator>> utilityEstimators;

            @Inject
            ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;

            @Override
            public UtilityEstimator get() {

                Verify.verify(utilityEstimators.containsKey(configGroup.mode));

                UtilityEstimator delegate = utilityEstimators.get(configGroup.mode).get();

                return new UtilityEstimator() {
                    @Override
                    public double estimateUtility(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
                        Id<Link> parkingLink = null;
                        for(int i=elements.size()-1; i>=0; i--) {
                            if((elements.get(i) instanceof Leg leg) && leg.getMode().equals(configGroup.mode)) {
                                parkingLink = leg.getRoute().getEndLinkId();
                            }
                        }
                        Verify.verify(parkingLink != null);
                        return delegate.estimateUtility(person, trip, elements); //- parkingAwareCarPenaltyProvider.getPenalty(person.getId(), parkingLink, );
                    }
                };
            }
        });

        bind(ParkingAwareCarPenaltyProvider.class).to(switch (modeChoiceConfigGroup.getPenaltyType()) {
            case DETAILED -> DetailedParkingAwareCarPenaltyProvider.class;
            case ZERO -> NoPenalty.class;
            default -> throw new IllegalStateException("Unexpected value: " + modeChoiceConfigGroup.getPenaltyType());
        });
    }

    @Provides
    public ParkingAwareCumulativeTourEstimator provideParkingAwareCumulativeTourEstimator(TimeInterpretation timeInterpretation, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic, TripEstimator tripEstimator, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, ParkingAwareCarPenaltyProvider parkingAwareCarPenaltyProvider) {
        return new ParkingAwareCumulativeTourEstimator(tripEstimator, timeInterpretation, configGroup.mode, parkingSpaceAssignmentLogic, networkWideParkingSpaceStore, parkingAwareCarPenaltyProvider);
    }

}
