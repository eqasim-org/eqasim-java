package org.matsim.contrib.shared_mobility.routing;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.NetworkRoutingInclAccessEgressModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class SharingRoutingModule implements RoutingModule {
	private final RoutingModule accessEgressRoutingModule;
	private final RoutingModule mainModeRoutingModule;

	private final InteractionFinder interactionFinder;
	private final Network network;
	private final PopulationFactory populationFactory;
	private final TimeInterpretation timeInterpretation;
	private final Vehicles vehicles;

	private final Id<SharingService> serviceId;

	public SharingRoutingModule(Scenario scenario, RoutingModule accessEgressRoutingModule,
			RoutingModule mainModeRoutingModule, InteractionFinder interactionFinder, Id<SharingService> serviceId,
			TimeInterpretation timeInterpretation) {
		this.interactionFinder = interactionFinder;
		this.accessEgressRoutingModule = accessEgressRoutingModule;
		this.mainModeRoutingModule = createPureNetworkRouter(scenario, mainModeRoutingModule);
		this.network = scenario.getNetwork();
		this.serviceId = serviceId;
		this.populationFactory = scenario.getPopulation().getFactory();
		this.timeInterpretation = timeInterpretation;
		this.vehicles = scenario.getVehicles();
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();

		List<PlanElement> allElements = new LinkedList<>();

		Optional<InteractionPoint> pickupInteraction = interactionFinder.findPickup(fromFacility);
		Optional<InteractionPoint> dropoffInteraction = interactionFinder.findDropoff(toFacility);

		if (pickupInteraction.isEmpty() || dropoffInteraction.isEmpty()) {
			return null;
		}

		InteractionPoint a = pickupInteraction.get();
		InteractionPoint b = dropoffInteraction.get();
		if (a.equals(b)) {
			return null;
		}

		// Create walk-out-of-building stage
		List<? extends PlanElement> exitElements = routeAccessEgressStage(fromFacility.getLinkId(),
				fromFacility.getLinkId(), departureTime, person, request.getAttributes());
		allElements.addAll(exitElements);
		// Create activity where the vehicle is searched for
		Activity bookActivity = createBookingActivity(departureTime, fromFacility.getLinkId());
		bookActivity.setStartTime(departureTime);
		allElements.add(bookActivity);

		// Route pickup stage

		List<? extends PlanElement> pickupElements = routeAccessEgressStage(fromFacility.getLinkId(),
				pickupInteraction.get().getLinkId(), departureTime, person, request.getAttributes());
		allElements.addAll(pickupElements);
		departureTime = timeInterpretation.decideOnElementsEndTime(pickupElements, departureTime).seconds();

		// Pickup activity
		Activity pickupActivity = createPickupActivity(departureTime, pickupInteraction.get());
		pickupActivity.setStartTime(departureTime);
		allElements.add(pickupActivity);
		departureTime = timeInterpretation.decideOnElementEndTime(pickupActivity, departureTime).seconds();

		// Route main stage

		List<? extends PlanElement> mainElements = routeMainStage(pickupActivity.getLinkId(),
				dropoffInteraction.get().getLinkId(), departureTime, person, request.getAttributes());
		allElements.addAll(mainElements);
		departureTime = timeInterpretation.decideOnElementsEndTime(pickupElements, departureTime).seconds();

		// Dropoff activity
		Activity dropoffActivity = createDropoffActivity(departureTime, dropoffInteraction.get());
		dropoffActivity.setStartTime(departureTime);
		allElements.add(dropoffActivity);
		departureTime = timeInterpretation.decideOnElementEndTime(dropoffActivity, departureTime).seconds();

		// Route dropoff stage

		List<? extends PlanElement> dropoffElements = routeAccessEgressStage(dropoffActivity.getLinkId(),
				toFacility.getLinkId(), departureTime, person, request.getAttributes());
		allElements.addAll(dropoffElements);

		return allElements;
	}

	// TODO: The following two functions are almost an exact replicate of the
	// functions in UserLogic. Try to conslidate.

	private List<? extends PlanElement> routeAccessEgressStage(Id<Link> originId, Id<Link> destinationId,
			double departureTime, Person person, Attributes tripAttributes) {
		Facility originFacility = new LinkWrapperFacility(network.getLinks().get(originId));
		Facility destinationFacility = new LinkWrapperFacility(network.getLinks().get(destinationId));

		return accessEgressRoutingModule.calcRoute(
				DefaultRoutingRequest.of(originFacility, destinationFacility, departureTime, person, tripAttributes));
	}

	private List<? extends PlanElement> routeMainStage(Id<Link> originId, Id<Link> destinationId, double departureTime,
			Person person, Attributes tripAttributes) {
		Facility originFacility = new LinkWrapperFacility(network.getLinks().get(originId));
		Facility destinationFacility = new LinkWrapperFacility(network.getLinks().get(destinationId));
	
		List<? extends PlanElement> elements = mainModeRoutingModule.calcRoute(
				DefaultRoutingRequest.of(originFacility, destinationFacility, departureTime, person, tripAttributes));
		assignPlaceholderVehicle(elements, originId, destinationId, person);
		return elements;
	}

	private void assignPlaceholderVehicle(List<? extends PlanElement> elements, Id<Link> originId,
			Id<Link> destinationId, Person person) {
		if (elements == null) {
			return;
		}

		for (PlanElement element : elements) {
			if (element instanceof Leg leg && leg.getRoute() instanceof NetworkRoute route) {
				// Local override: the upstream contrib picked an existing sharing vehicle
				// from scenario vehicles by matching initialLinkId. That forced every
				// sharing vehicle to also exist in vehicles.xml at routing time. Here we
				// only assign a stable placeholder vehicle for the pickup link. During
				// QSim, SharingLogic replaces this placeholder with the actually reserved
				// sharing vehicle once the service has selected one.
				Id<Vehicle> vehicleId = getPlaceholderVehicleId(leg.getMode(), originId);
				ensurePlaceholderVehicle(vehicleId, originId, leg.getMode());
				route.setVehicleId(vehicleId);
			}
		}
	}

	private Id<Vehicle> getPlaceholderVehicleId(String mode, Id<Link> linkId) {
		// One placeholder per service/mode/link is enough to satisfy MATSim's route
		// vehicle-id requirement without encoding a concrete bike/car choice too early.
		return Id.createVehicleId("sharing_placeholder_" + serviceId + "_" + mode + "_" + linkId);
	}

	private void ensurePlaceholderVehicle(Id<Vehicle> vehicleId, Id<Link> linkId, String mode) {
		Vehicle vehicle = vehicles.getVehicles().get(vehicleId);
		if (vehicle != null) {
			Id<Link> initialLinkId = VehicleUtils.getInitialLinkId(vehicle);
			if (initialLinkId != null && !initialLinkId.equals(linkId)) {
				throw new IllegalStateException("Sharing placeholder vehicle " + vehicleId
						+ " already exists at initialLinkId " + initialLinkId + " but is needed at " + linkId);
			}
			return;
		}

		// The placeholder is added only to keep route and vehicle-container
		// consistency for planning. It is not meant to represent physical sharing
		// supply; physical sharing vehicles are inserted by SharingVehicleSource.
		Id<VehicleType> vehicleTypeId = Id.create("default_" + mode, VehicleType.class);
		VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);
		if (vehicleType == null) {
			vehicleType = vehicles.getFactory().createVehicleType(vehicleTypeId);
			vehicleType.setNetworkMode(mode);
			vehicles.addVehicleType(vehicleType);
		}

		vehicle = vehicles.getFactory().createVehicle(vehicleId, vehicleType);
		VehicleUtils.setInitialLinkId(vehicle, linkId);
		vehicles.addVehicle(vehicle);
	}

	private static RoutingModule createPureNetworkRouter(Scenario scenario, RoutingModule mainModeRoutingModule) {
		if (mainModeRoutingModule instanceof NetworkRoutingInclAccessEgressModule) {
			try {
				String mode = getField(mainModeRoutingModule, "mode", String.class);
				Network filteredNetwork = getField(mainModeRoutingModule, "filteredNetwork", Network.class);
				LeastCostPathCalculator routeAlgo = getField(mainModeRoutingModule, "routeAlgo",
						LeastCostPathCalculator.class);

				return DefaultRoutingModules.createPureNetworkRouter(mode, scenario.getPopulation().getFactory(),
						filteredNetwork, routeAlgo);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Could not create pure network router for shared mobility.", e);
			}
		}

		return mainModeRoutingModule;
	}

	private static <T> T getField(Object object, String name, Class<T> type) throws ReflectiveOperationException {
		Field field = object.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return type.cast(field.get(object));
	}

	private Activity createBookingActivity(double now, Id<Link> linkId) {
		Activity activity = populationFactory.createActivityFromLinkId(SharingUtils.BOOKING_ACTIVITY, linkId);
		activity.setStartTime(now);
		activity.setMaximumDuration(SharingUtils.INTERACTION_DURATION);
		SharingUtils.setServiceId(activity, serviceId);
		return activity;
	}

	private Activity createPickupActivity(double now, InteractionPoint interaction) {
		Activity activity = populationFactory.createActivityFromLinkId(SharingUtils.PICKUP_ACTIVITY,
				interaction.getLinkId());
		activity.setStartTime(now);
		activity.setMaximumDuration(SharingUtils.INTERACTION_DURATION);
		SharingUtils.setServiceId(activity, serviceId);

		if (interaction.isStation()) {
			SharingUtils.setStationId(activity, interaction.getStationId().get());
		}

		return activity;
	}

	private Activity createDropoffActivity(double now, InteractionPoint interaction) {
		Activity activity = populationFactory.createActivityFromLinkId(SharingUtils.DROPOFF_ACTIVITY,
				interaction.getLinkId());
		activity.setStartTime(now);
		activity.setMaximumDuration(SharingUtils.INTERACTION_DURATION);
		SharingUtils.setServiceId(activity, serviceId);

		if (interaction.isStation()) {
			SharingUtils.setStationId(activity, interaction.getStationId().get());
		}

		return activity;
	}
}
