package org.eqasim.switzerland.ch_cmdp.routing;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
		MessageDigest digest;
		double maximumValue;

		try {
			digest = MessageDigest.getInstance("SHA-512");
			maximumValue = BigInteger.valueOf(2).pow(digest.getDigestLength() * 8).doubleValue();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(
					"Cannot find SHA-512 algorithm. Providing intermodal utility errors is not possible.");
		}

		// Hashing instead of drawing from a shared RNG keeps results independent of
		// routing order and thread scheduling.
		digest.update(ByteBuffer.allocate(Long.BYTES).putLong(randomSeed).array());
		digest.update((byte) 0);
		digest.update(person.getId().toString().getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
		digest.update(mode.getBytes(StandardCharsets.UTF_8));

		double value = new BigInteger(1, digest.digest()).doubleValue();
		double uniform = value / maximumValue;

		if (uniform <= 0.0) {
			return Double.MIN_VALUE;
		}

		if (uniform >= 1.0) {
			return Math.nextDown(1.0);
		}

		return uniform;
	}
}
