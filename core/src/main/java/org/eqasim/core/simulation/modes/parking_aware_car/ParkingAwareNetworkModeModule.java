package org.eqasim.core.simulation.modes.parking_aware_car;

import com.google.inject.*;
import org.eqasim.core.simulation.modes.parking_aware_car.config.ParkingAwareNetworkModeConfigGroup;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageControlerListener;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageEventListener;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogicParameterSet;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based.PersonAttributeBasedParkingAssignmentConfigGroup;
import org.eqasim.core.simulation.modes.parking_aware_car.routing.ParkingAwareNetworkRoutingModule;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based.PersonAttributeBasedParkingAssignment;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.RoutingModule;


public class ParkingAwareNetworkModeModule extends AbstractModule {

    public ParkingAwareNetworkModeModule() {

    }

    @Override
    public void install() {

        ParkingAwareNetworkModeConfigGroup configGroup = (ParkingAwareNetworkModeConfigGroup) getConfig().getModules().get(ParkingAwareNetworkModeConfigGroup.GROUP_NAME);

        ParkingSpaceAssignmentLogicParameterSet.ParkingAssignmentLogicParams parkingAssignmentLogicParams = configGroup.getParkingSpaceAssignmentLogicParams();

        if(parkingAssignmentLogicParams instanceof PersonAttributeBasedParkingAssignmentConfigGroup personAttributeBasedParkingAssignmentConfigGroup) {
            bind(ParkingSpaceAssignmentLogic.class).toProvider(new Provider<>() {

                @Inject
                private NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

                @Inject
                private Population population;

                @Inject
                private Network network;

                @Override
                public ParkingSpaceAssignmentLogic get() {
                    return new PersonAttributeBasedParkingAssignment(personAttributeBasedParkingAssignmentConfigGroup.getOrderedParkingTypes(),
                            personAttributeBasedParkingAssignmentConfigGroup.getParkingTypesAvailableForEveryone(), networkWideParkingSpaceStore, population, network);
                }
            });
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

            @Override
            public ParkingUsageEventListener get() {
                return new ParkingUsageEventListener(configGroup.mode, configGroup.parkingUsageAggregationInterval, networkWideParkingSpaceStore, parkingSpaceAssignmentLogic);
            }
        }).asEagerSingleton();

        bind(ParkingUsageControlerListener.class).toProvider(new Provider<>() {

            @Inject
            private ParkingUsageEventListener parkingUsageEventListener;

            @Inject
            private OutputDirectoryHierarchy outputDirectoryHierarchy;

            @Override
            public ParkingUsageControlerListener get() {
                return new ParkingUsageControlerListener(parkingUsageEventListener, outputDirectoryHierarchy);
            }
        });

        addEventHandlerBinding().to(ParkingUsageEventListener.class);
        addControlerListenerBinding().to(ParkingUsageControlerListener.class);


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
    }

    @Provides
    @Singleton
    public NetworkWideParkingSpaceStore provideNetworkWideParkingSpaceStore(Network network) {
        return new NetworkWideParkingSpaceStore(network);
    }
}
