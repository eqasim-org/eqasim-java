package org.eqasim.switzerland.ch_cmdp.routing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.inject.Inject;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder.Direction;

public class SwissStochasticIntermodalAccessEgress implements RaptorIntermodalAccessEgress {
	private final long randomSeed;
	private final double scale;
	private final Set<String> modes;

	@Inject
	public SwissStochasticIntermodalAccessEgress(GlobalConfigGroup globalConfig,
			SwissIntermodalAccessEgressConfigGroup config) {
		this(globalConfig.getRandomSeed(), config.getUtilityErrorScale(), config.getUtilityErrorModes());
	}

	SwissStochasticIntermodalAccessEgress(long randomSeed, double scale, Set<String> modes) {
		this.randomSeed = randomSeed;
		this.scale = scale;
		this.modes = Set.copyOf(modes);
	}

	@Override
	public RIntermodalAccessEgress calcIntermodalAccessEgress(List<? extends PlanElement> legs, RaptorParameters params,
			Person person, Direction direction) {
		double disutility = 0.0;
		double travelTime = 0.0;
		Set<String> stochasticModes = new HashSet<>();

		for (PlanElement element : legs) {
			if (element instanceof Leg leg) {
				String mode = leg.getMode();
				OptionalTime legTravelTime = leg.getTravelTime();
				if (legTravelTime.isDefined()) {
					travelTime += legTravelTime.seconds();
					disutility += legTravelTime.seconds() * -params.getMarginalUtilityOfTravelTime_utl_s(mode);
				}

				if (isStochasticMode(mode)) {
					stochasticModes.add(mode);
				}
			} else if (element instanceof Activity activity) {
				if (activity.getMaximumDuration().isDefined()) {
					travelTime += activity.getMaximumDuration().seconds();
				}
			}
		}

		// SwissRailRaptor minimizes disutility. The Gumbel term is a utility shock,
		// so a positive draw makes this access/egress mode cheaper for this person.
		if (scale > 0.0) {
			for (String mode : stochasticModes) {
				disutility += getGumbelUtility(person, mode);
			}
		}

		return new RIntermodalAccessEgress(legs, disutility, travelTime, direction);
	}

	private boolean isStochasticMode(String mode) {
		return modes.isEmpty() || modes.contains(mode);
	}

	private double getGumbelUtility(Person person, String mode) {
		double uniform = getUniform(person, mode);
		return -scale * Math.log(-Math.log(uniform));
	}

	private double getUniform(Person person, String mode) {
		// Derive a stable person/mode-specific draw without a shared RNG. This keeps
		// stochastic intermodal access costs independent of routing order and thread
		// scheduling while staying much cheaper than constructing a cryptographic hash.
		long value = randomSeed;
		value ^= Integer.toUnsignedLong(person.getId().toString().hashCode()) * 0x9E3779B97F4A7C15L;
		value ^= Long.rotateLeft(Integer.toUnsignedLong(mode.hashCode()) * 0xBF58476D1CE4E5B9L, 32);

		return ((splitMix64(value) >>> 11) + 0.5) * 0x1.0p-53;
	}

	static private long splitMix64(long value) {
		// SplitMix64 finalization scrambles nearby inputs into well-distributed bits;
		// the top 53 bits are then mapped to the open interval (0, 1).
		value += 0x9E3779B97F4A7C15L;
		value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
		value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
		return value ^ (value >>> 31);
	}
}
