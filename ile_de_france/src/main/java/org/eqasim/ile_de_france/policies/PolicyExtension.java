package org.eqasim.ile_de_france.policies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.eqasim.ile_de_france.policies.city_tax.CityTaxPolicyExtension;
import org.eqasim.ile_de_france.policies.city_tax.CityTaxPolicyFactory;
import org.eqasim.ile_de_france.policies.limited_traffic_zone.LimitedTrafficZonePolicyExtension;
import org.eqasim.ile_de_france.policies.limited_traffic_zone.LimitedTrafficZonePolicyFactory;
import org.eqasim.ile_de_france.policies.mode_choice.PolicyUtilityEstimator;
import org.eqasim.ile_de_france.policies.mode_choice.SumUtilityPenalty;
import org.eqasim.ile_de_france.policies.mode_choice.UtilityPenalty;
import org.eqasim.ile_de_france.policies.routing.PolicyTravelDisutilityFactory;
import org.eqasim.ile_de_france.policies.routing.RoutingPenalty;
import org.eqasim.ile_de_france.policies.routing.SumRoutingPenalty;
import org.eqasim.ile_de_france.policies.transit_discount.TransitDiscountPolicyExtension;
import org.eqasim.ile_de_france.policies.transit_discount.TransitDiscountPolicyFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;

import com.google.common.base.Verify;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class PolicyExtension extends AbstractEqasimExtension {
	private final static String ESTIMATOR_PREFIX = "policy:";

	private String delegateCarEstimator;
	private String delegateTransitEstimator;

	public void adaptConfiguration(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		delegateCarEstimator = eqasimConfig.getEstimators().get(TransportMode.car);
		delegateTransitEstimator = eqasimConfig.getEstimators().get(TransportMode.pt);

		delegateCarEstimator = delegateCarEstimator.replace(ESTIMATOR_PREFIX, "");
		delegateTransitEstimator = delegateTransitEstimator.replace(ESTIMATOR_PREFIX, "");

		eqasimConfig.setEstimator(TransportMode.car, ESTIMATOR_PREFIX + delegateCarEstimator);
		eqasimConfig.setEstimator(TransportMode.pt, ESTIMATOR_PREFIX + delegateTransitEstimator);
	}

	@Override
	protected void installEqasimExtension() {
		Verify.verifyNotNull(delegateCarEstimator, "Need to run PolicyExtension.adaptConfiguration first");
		Verify.verifyNotNull(delegateTransitEstimator, "Need to run PolicyExtension.adaptConfiguration first");

		// set up travel disutility for routing
		addTravelDisutilityFactoryBinding(TransportMode.car).to(PolicyTravelDisutilityFactory.class);
		addTravelDisutilityFactoryBinding("car_passenger").to(OnlyTimeDependentTravelDisutilityFactory.class);

		install(new CityTaxPolicyExtension());
		install(new LimitedTrafficZonePolicyExtension());
		install(new TransitDiscountPolicyExtension());

		var policyBinder = MapBinder.newMapBinder(binder(), String.class, PolicyFactory.class);
		policyBinder.addBinding(CityTaxPolicyFactory.POLICY_NAME).to(CityTaxPolicyFactory.class);
		policyBinder.addBinding(LimitedTrafficZonePolicyFactory.POLICY_NAME).to(LimitedTrafficZonePolicyFactory.class);
		policyBinder.addBinding(TransitDiscountPolicyFactory.POLICY_NAME).to(TransitDiscountPolicyFactory.class);

		bindUtilityEstimator(ESTIMATOR_PREFIX + delegateCarEstimator)
				.to(Key.get(PolicyUtilityEstimator.class, Names.named(TransportMode.car)));

		bindUtilityEstimator(ESTIMATOR_PREFIX + delegateTransitEstimator)
				.to(Key.get(PolicyUtilityEstimator.class, Names.named(TransportMode.pt)));
	}

	@Provides
	@Singleton
	Map<String, Policy> providePolicies(Map<String, PolicyFactory> factories, Population population) {
		PoliciesConfigGroup policyConfig = PoliciesConfigGroup.get(getConfig());
		Map<String, Policy> policies = new HashMap<>();

		Set<String> names = new HashSet<>();

		if (policyConfig != null) {
			for (var collection : policyConfig.getParameterSets().values()) {
				for (var raw : collection) {
					PolicyConfigGroup policy = (PolicyConfigGroup) raw;

					if (policy.active) {
						Verify.verify(policy.policyName != null && policy.policyName.length() > 0,
								"Policy names must be set");

						if (!names.add(policy.policyName)) {
							throw new IllegalStateException("Duplicate policy name: " + policy.policyName);
						}

						PolicyPersonFilter filter = PolicyPersonFilter.create(population, policy);

						policies.put(policy.policyName,
								factories.get(policy.getName()).createPolicy(policy.policyName, filter));
					}
				}
			}
		}

		return policies;
	}

	@Provides
	@Singleton
	PolicyTravelDisutilityFactory providePolicyTravelDisutilityFactory(RoutingPenalty linkPenalty) {
		return new PolicyTravelDisutilityFactory(linkPenalty);
	}

	@Provides
	@Named(TransportMode.car)
	PolicyUtilityEstimator providePolicyUtilityEstimatorForCar(Map<String, Provider<UtilityEstimator>> providers,
			UtilityPenalty penalty) {
		UtilityEstimator delegate = providers.get(delegateCarEstimator).get();
		return new PolicyUtilityEstimator(delegate, penalty, TransportMode.car);
	}

	@Provides
	@Named(TransportMode.pt)
	PolicyUtilityEstimator providePolicyUtilityEstimatorForTransit(Map<String, Provider<UtilityEstimator>> providers,
			UtilityPenalty penalty) {
		UtilityEstimator delegate = providers.get(delegateTransitEstimator).get();
		return new PolicyUtilityEstimator(delegate, penalty, TransportMode.pt);
	}

	@Provides
	UtilityPenalty provideUtilityPenalty(Map<String, Policy> policies) {
		List<UtilityPenalty> penalties = new LinkedList<>();

		for (Policy policy : policies.values()) {
			UtilityPenalty penalty = policy.getUtilityPenalty();

			if (penalty != null) {
				penalties.add(penalty);
			}
		}

		return new SumUtilityPenalty(penalties);
	}

	@Provides
	RoutingPenalty provideRoutingPenalty(Map<String, Policy> policies) {
		List<RoutingPenalty> penalties = new LinkedList<>();

		for (Policy policy : policies.values()) {
			RoutingPenalty penalty = policy.getRoutingPenalty();

			if (penalty != null) {
				penalties.add(penalty);
			}
		}

		return new SumRoutingPenalty(penalties);
	}
}
