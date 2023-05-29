package org.eqasim.ile_de_france.routing;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class IDFRaptorParameters implements ParameterDefinition {
	public double maximumTransferDistance_m = 400.0;
	public double walkSpeed_m_s = 1.33;
	public double walkFactor = 1.3;

	public double minimalTransferTime_s = 60;
	public double transferWalkMargin_s = 5;

	public double directWalkFactor = 100.0;

	public double transferUtility = -0.26980996526677087;
	public double waitingUtility_h = -1.298754292554342;

	public double railUtility_h = -0.4543829479956706;
	public double subwayUtility_h = -0.7715570079250351;
	public double tramUtility_h = -1.7608452482684784;
	public double busUtility_h = -1.7447089000006268;

	public double walkUtility_h = -1.6352586824349615;
	public double otherUtility_h = -1.0;
}
