package org.eqasim.ile_de_france.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class IDFModeParameters extends ModeParameters {
	public class IDFCarParameters {
		public double betaParkingPressure_u;
	}

	public final IDFCarParameters idfCar = new IDFCarParameters();

	public class IDFCarPassengerParameters {
		public double alpha_u;
		public double betaInVehicleTravelTime_u_min;
		public double betaDrivingPermit_u;
	}

	public final IDFCarPassengerParameters idfCarPassenger = new IDFCarPassengerParameters();

	public class IDFPtParameters {
		public double betaDrivingPermit_u;
		public double betaOnlyBus_u;
		public double betaCrossingParisBorder_u;
	}

	public final IDFPtParameters idfPt = new IDFPtParameters();

	public double betaAccessTime_u_min;

	public double referenceIncomePerCU_EUR;
	public double lambdaCostIncome;

	public double betaRoadInsideParis_u;

	public static IDFModeParameters buildDefault() {
		IDFModeParameters parameters = new IDFModeParameters();

		// Access
		parameters.betaAccessTime_u_min = -0.021105;

		// Cost
		parameters.betaCost_u_MU = -0.169591;

		parameters.lambdaCostEuclideanDistance = 0.174056;
		parameters.referenceEuclideanDistance_km = 4.329534430285437;

		parameters.lambdaCostIncome = -0.131802;
		parameters.referenceIncomePerCU_EUR = 1842.3492427549477;

		// Car
		parameters.car.alpha_u = 1.164972;
		parameters.car.betaTravelTime_u_min = -0.042989;

		parameters.idfCar.betaParkingPressure_u = -1.274770;

		// Car passenger
		parameters.idfCarPassenger.alpha_u = -0.340312;
		parameters.idfCarPassenger.betaDrivingPermit_u = -1.206877;
		parameters.idfCarPassenger.betaInVehicleTravelTime_u_min = -0.070463;

		// Road
		parameters.betaRoadInsideParis_u = -1.513682;

		// PT
		parameters.pt.alpha_u = 0.0;
		parameters.pt.betaLineSwitch_u = -0.263965;
		parameters.pt.betaInVehicleTime_u_min = -0.007223;
		parameters.pt.betaWaitingTime_u_min = -0.034504;

		parameters.idfPt.betaDrivingPermit_u = -0.955961;
		parameters.idfPt.betaOnlyBus_u = -0.748072;

		parameters.idfPt.betaCrossingParisBorder_u = 0.934523;

		// Bike
		parameters.bike.alpha_u = -2.283258;
		parameters.bike.betaTravelTime_u_min = -0.050672;

		// Walk
		parameters.walk.alpha_u = 2.369931;
		parameters.walk.betaTravelTime_u_min = -0.114553;

		return parameters;
	}
}
