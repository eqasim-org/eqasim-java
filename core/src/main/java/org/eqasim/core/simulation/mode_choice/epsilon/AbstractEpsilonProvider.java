package org.eqasim.core.simulation.mode_choice.epsilon;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public abstract class AbstractEpsilonProvider implements EpsilonProvider {
	private final long randomSeed;

	public AbstractEpsilonProvider(long randomSeed) {
		this.randomSeed = randomSeed;
	}

	protected double getUniformEpsilon(Id<Person> personId, int tripIndex, String mode) {

		MessageDigest digest;
		double maximumValue;
		try {
			digest = MessageDigest.getInstance("SHA-512");
			maximumValue = BigInteger.valueOf(2).pow(digest.getDigestLength() * 8).doubleValue();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Cannot find SHA-512 algorithm. Providing epsilons is not possible.");
		}

		digest.reset();

		digest.update(ByteBuffer //
				.allocate(8 + 3 * 4) // long + 3 * int
				.putLong(randomSeed) //
				.putInt(personId.index()) //
				.putInt(tripIndex) //
				.putInt(mode.hashCode()) //
				.array() //
		);

		return new BigInteger(1, digest.digest()).doubleValue() / maximumValue;
	}
}
