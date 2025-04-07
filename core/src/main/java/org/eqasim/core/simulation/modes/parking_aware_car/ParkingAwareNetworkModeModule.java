package org.eqasim.core.simulation.modes.parking_aware_car;

import com.google.common.base.Verify;
import com.google.inject.*;
import org.eqasim.core.simulation.modes.parking_aware_car.config.ParkingAwareNetworkModeConfigGroup;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageControlerListener;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageEventListener;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingsWriterControlerListener;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.ParkingAwareCumulativeTourEstimator;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.ParkingAwareNetworkModeChoiceModule;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogicParameterSet;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based.PersonAttributeBasedParkingAssignmentConfigGroup;
import org.eqasim.core.simulation.modes.parking_aware_car.routing.ParkingAwareMultimodalLinkChooser;
import org.eqasim.core.simulation.modes.parking_aware_car.routing.ParkingAwareNetworkRoutingModule;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based.PersonAttributeBasedParkingAssignment;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.RoutingModule;

import java.util.Optional;


public class ParkingAwareNetworkModeModule extends AbstractModule {

    public ParkingAwareNetworkModeModule() {

    }

    @Override
    public void install() {

        Verify.verify(((DiscreteModeChoiceConfigGroup) getConfig().getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME)).getTourEstimator().equals(ParkingAwareCumulativeTourEstimator.NAME));

        ParkingAwareNetworkModeConfigGroup configGroup = (ParkingAwareNetworkModeConfigGroup) getConfig().getModules().get(ParkingAwareNetworkModeConfigGroup.GROUP_NAME);
        Verify.verify(!((DiscreteModeChoiceConfigGroup) getConfig().getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME)).getCachedModes().contains(configGroup.mode));

        ParkingSpaceAssignmentLogicParameterSet.ParkingAssignmentLogicParams parkingAssignmentLogicParams = configGroup.getParkingSpaceAssignmentLogicParams();

        if(parkingAssignmentLogicParams instanceof PersonAttributeBasedParkingAssignmentConfigGroup personAttributeBasedParkingAssignmentConfigGroup) {
            bind(ParkingSpaceAssignmentLogic.class).toProvider(new Provider<>() {

                @Inject
                private NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

                @Inject
                private Population population;


                @Override
                public ParkingSpaceAssignmentLogic get() {
                    return new PersonAttributeBasedParkingAssignment(personAttributeBasedParkingAssignmentConfigGroup.getOrderedParkingTypes(),
                            personAttributeBasedParkingAssignmentConfigGroup.getParkingTypesAvailableForEveryone(), networkWideParkingSpaceStore, population);
                }
            }).asEagerSingleton();
        } else {
            throw new IllegalStateException("Unknown parkingAssignment strategy");
        }

        bind(NetworkRoutingProvider.class).toInstance(new NetworkRoutingProvider("car"));

        /*addRoutingModuleBinding("car").toProvider(new ParkingAwareNetworkRoutingModuleProvider("car",
                new NetworkRoutingProvider("car"),
                getProvider(TypeLiteral.get(NetworkWideParkingSpaceStore.class)),
                getProvider(TypeLiteral.get(ParkingSpaceAssignmentLogic.class))));*/

        bind(ParkingUsageEventListener.class).toProvider(new Provider<>() {

            @Inject
            private NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
            @Inject
            private ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;
            @Inject
            private QSimConfigGroup qSimConfigGroup;
            @Inject
            private Population population;

            @Override
            public ParkingUsageEventListener get() {
                return new ParkingUsageEventListener(configGroup.mode, configGroup.parkingUsageAggregationInterval, networkWideParkingSpaceStore, parkingSpaceAssignmentLogic, Optional.ofNullable(qSimConfigGroup).map(cfg -> cfg.getEndTime().seconds()).orElse(30.0 * 3600), population);
            }
        }).asEagerSingleton();

        bind(ParkingUsageControlerListener.class).toProvider(new Provider<>() {

            @Inject
            private ParkingUsageEventListener parkingUsageEventListener;

            @Inject
            private OutputDirectoryHierarchy outputDirectoryHierarchy;

            @Inject
            private NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

            @Override
            public ParkingUsageControlerListener get() {
                return new ParkingUsageControlerListener(parkingUsageEventListener, outputDirectoryHierarchy, networkWideParkingSpaceStore);
            }
        });

        bind(ParkingsWriterControlerListener.class).toProvider(new Provider<>() {

            @Inject
            Network network;

            @Inject
            OutputDirectoryHierarchy outputDirectoryHierarchy;

            @Inject
            NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

            @Override
            public ParkingsWriterControlerListener get() {
                return new ParkingsWriterControlerListener(network, networkWideParkingSpaceStore, outputDirectoryHierarchy);
            }
        });

        addEventHandlerBinding().to(ParkingUsageEventListener.class);
        addMobsimListenerBinding().to(ParkingUsageEventListener.class);
        addControlerListenerBinding().to(ParkingUsageControlerListener.class);
        addControlerListenerBinding().to(ParkingsWriterControlerListener.class).asEagerSingleton();


        addRoutingModuleBinding(configGroup.mode).toProvider(new Provider<>() {

            @Inject
            private NetworkRoutingProvider networkRoutingProvider;

            @Inject
            private NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

            @Inject
            private ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;

            @Override
            public RoutingModule get() {
                return new ParkingAwareNetworkRoutingModule(configGroup.mode, networkRoutingProvider.get(), networkWideParkingSpaceStore, parkingSpaceAssignmentLogic);
            }
        });

        if(configGroup.getParkingAwareNetworkModeChoiceConfigGroup() != null) {
            install(new ParkingAwareNetworkModeChoiceModule(configGroup));
        }

        bind(MultimodalLinkChooser.class).to(ParkingAwareMultimodalLinkChooser.class);
        bind(ParkingAwareMultimodalLinkChooser.class).toProvider(new Provider<ParkingAwareMultimodalLinkChooser>() {

            @Inject
            private NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

            @Inject
            private ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;

            @Inject
            private ParkingUsageEventListener parkingUsageEventListener;

            @Inject
            private Network network;

            @Override
            public ParkingAwareMultimodalLinkChooser get() {
                return new ParkingAwareMultimodalLinkChooser(networkWideParkingSpaceStore, network, parkingSpaceAssignmentLogic, parkingUsageEventListener);
            }
        });
    }

    @Provides
    @Singleton
    public NetworkWideParkingSpaceStore provideNetworkWideParkingSpaceStore(Network network) {
        return new NetworkWideParkingSpaceStore(network);
    }
}
