package org.eqasim.core.simulation.modes.parking_aware_car;

import com.google.common.base.Verify;
import com.google.inject.*;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
import org.eqasim.core.simulation.modes.parking_aware_car.config.ParkingAwareNetworkModeConfigGroup;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageControlerListener;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageEventListener;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingsWriterControlerListener;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.ParkingAwareCumulativeTourEstimator;
import org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.ParkingAwareNetworkModeChoiceModule;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogicParameterSet;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based.PersonAttributeBasedParkingAssignmentConfigGroup;
import org.eqasim.core.simulation.modes.parking_aware_car.routing.*;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based.PersonAttributeBasedParkingAssignment;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;


public class ParkingAwareNetworkModeModule extends AbstractModule {

    public ParkingAwareNetworkModeModule() {

    }

    @Override
    public void install() {

        Verify.verify(((DiscreteModeChoiceConfigGroup) getConfig().getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME)).getTourEstimator().equals(ParkingAwareCumulativeTourEstimator.NAME));

        ParkingAwareNetworkModeConfigGroup configGroup = (ParkingAwareNetworkModeConfigGroup) getConfig().getModules().get(ParkingAwareNetworkModeConfigGroup.GROUP_NAME);
        Verify.verify(!((DiscreteModeChoiceConfigGroup) getConfig().getModules().get(DiscreteModeChoiceConfigGroup.GROUP_NAME)).getCachedModes().contains(configGroup.mode));

        RoutingConfigGroup routingConfigGroup = getConfig().routing();
        Verify.verify(routingConfigGroup.getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink) || routingConfigGroup.getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.accessEgressModeToLinkPlusTimeConstant));

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
            @Inject
            private Households households;
            @Inject ModeAvailability modeAvailability;

            @Override
            public ParkingUsageEventListener get() {
                return new ParkingUsageEventListener(configGroup.mode, configGroup.parkingUsageAggregationInterval, networkWideParkingSpaceStore, parkingSpaceAssignmentLogic, Optional.ofNullable(qSimConfigGroup).map(cfg -> cfg.getEndTime().seconds()).orElse(30.0 * 3600), population, households, modeAvailability);
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

        bind(ParkingSpaceFinder.class).toProvider(new Provider<>() {

            @Inject
            private NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

            @Inject
            private ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;

            @Inject
            private ParkingUsageEventListener parkingUsageEventListener;

            @Inject
            private Network network;

            @Override
            public ParkingSpaceFinder get() {
                ScenarioExtent scenarioExtent = null;
                if(configGroup.parkingSearchRestrictionArea != null) {
                    try {
                        scenarioExtent = new ShapeScenarioExtent.Builder(new File(configGroup.parkingSearchRestrictionArea), Optional.empty(), Optional.empty()).build();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                //return new DefaultParkingSpaceFinder(networkWideParkingSpaceStore, network, parkingSpaceAssignmentLogic, parkingUsageEventListener, scenarioExtent, configGroup.assumedParkingDuration, configGroup.searchRadius);
                Set<String> activityTypes = Set.of("home");
                return new FixedParkingPerDestinationLinkParkingSpaceFinder(networkWideParkingSpaceStore, network, parkingSpaceAssignmentLogic, parkingUsageEventListener, scenarioExtent, configGroup.assumedParkingDuration, configGroup.searchRadius, activityTypes);
            }
        }).asEagerSingleton();

        bind(MultimodalLinkChooser.class).toProvider(new Provider<>() {

            @Inject
            private ParkingSpaceFinder parkingSpaceFinder;

            @Override
            public MultimodalLinkChooser get() {
                return new ParkingAwareMultimodalLinkChooser(parkingSpaceFinder, configGroup.mode);
            }
        });

        bind(InitialParkingAssignment.class).toProvider(new Provider<>() {

            @Inject
            private Population population;

            @Inject
            private ParkingSpaceFinder parkingSpaceFinder;

            @Inject
            private ActivityFacilities activityFacilities;

            @Override
            public InitialParkingAssignment get() {
                return new InitialParkingAssignment(population, parkingSpaceFinder, activityFacilities);
            }
        }).asEagerSingleton();
    }

    @Provides
    @Singleton
    public NetworkWideParkingSpaceStore provideNetworkWideParkingSpaceStore(SingleModeNetworksCache singleModeNetworksCache) {
        ParkingAwareNetworkModeConfigGroup configGroup = (ParkingAwareNetworkModeConfigGroup) getConfig().getModules().get(ParkingAwareNetworkModeConfigGroup.GROUP_NAME);
        return new NetworkWideParkingSpaceStore(singleModeNetworksCache.getOrCreateSingleModeNetwork(configGroup.mode));
    }
}
