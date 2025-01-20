package org.eqasim.core.simulation.policies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.policies.config.PoliciesConfigGroup;
import org.eqasim.core.simulation.policies.config.PolicyConfigGroup;
import org.eqasim.core.simulation.policies.impl.city_tax.CityTaxPolicyExtension;
import org.eqasim.core.simulation.policies.impl.city_tax.CityTaxPolicyFactory;
import org.eqasim.core.simulation.policies.impl.limited_traffic_zone.LimitedTrafficZonePolicyExtension;
import org.eqasim.core.simulation.policies.impl.limited_traffic_zone.LimitedTrafficZonePolicyFactory;
import org.eqasim.core.simulation.policies.impl.transit_discount.TransitDiscountPolicyExtension;
import org.eqasim.core.simulation.policies.impl.transit_discount.TransitDiscountPolicyFactory;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.eqasim.core.simulation.policies.routing.SumRoutingPenalty;
import org.eqasim.core.simulation.policies.utility.SumPenalty;
import org.eqasim.core.simulation.policies.utility.UtilityPenalty;
import org.matsim.api.core.v01.population.Population;

import com.google.common.base.Verify;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

public class PolicyExtension extends AbstractEqasimExtension {
	@Override
	protected void installEqasimExtension() {
		install(new CityTaxPolicyExtension());
		install(new LimitedTrafficZonePolicyExtension());
		install(new TransitDiscountPolicyExtension());

		var policyBinder = MapBinder.newMapBinder(binder(), String.class, PolicyFactory.class);
		policyBinder.addBinding(CityTaxPolicyFactory.POLICY_NAME).to(CityTaxPolicyFactory.class);
		policyBinder.addBinding(LimitedTrafficZonePolicyFactory.POLICY_NAME).to(LimitedTrafficZonePolicyFactory.class);
		policyBinder.addBinding(TransitDiscountPolicyFactory.POLICY_NAME).to(TransitDiscountPolicyFactory.class);
	}

	@Provides
	@Singleton
	Map<String, Policy> providePolicies(Map<String, PolicyFactory> factories, Population population) {
		PoliciesConfigGroup policyConfig = PoliciesConfigGroup.get(getConfig());
		Map<String, Policy> policies = new HashMap<>();

		if (policyConfig == null) {
			return policies;
		}

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
	UtilityPenalty provideUtilityPenalty(Map<String, Policy> policies) {
		List<UtilityPenalty> penalties = new LinkedList<>();

		for (Policy policy : policies.values()) {
			UtilityPenalty penalty = policy.getUtilityPenalty();

			if (penalty != null) {
				penalties.add(penalty);
			}
		}

		return new SumPenalty(penalties);
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
