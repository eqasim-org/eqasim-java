package org.eqasim.ile_de_france.mode_choice.epsilon;

public class GumbelEpsilonProvider extends AbstractEpsilonProvider {
	private final double scale;

	public GumbelEpsilonProvider(long randomSeed, double scale) {
		super(randomSeed);
		this.scale = scale;
	}

	@Override
	public double getEpsilon(int hash) {
		double u = getUniformEpsilon(hash);
		return -scale * Math.log(-Math.log(u));
	}
}
