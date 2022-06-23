package org.eqasim.examples.SMMFramework.generalizedSMMModeChoice.generalizedSMMModeChoice.variables_parameters;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;

public class SMMParameters extends ModeParameters {



    public class SharedMobilityParameters{

    }
    public class CarParameters {
        public double betaTravelTime_u_min = 0.0;
        public double accessTime=0.0;
        public double egressTime=0.0;
        public double betaAccess_Time = 0.0;
        public double betaEgress_Time=0.0;
        public double parkingTime=0.0;
        public double betaParkingTime_u_min = 0.0;
        public double sigma=0.0;
        public double pool=0.0;
    }

    public class PtParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double betaAccess_Time = 0.0;
        public double betaEgress_Time=0.0;
        public double betaTransfers=0.0;
        public double betaAge=0.0;
        public double betaBikeAcc=0.0;
        public double betaCarAcc=0.0;
        public double betaPTPass=0.0;
        public double sigma=0.0;
    }

    public class BikeParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double accessTime=0.0;
        public double egressTime=0.0;
        public double betaAccess_Time = 0.0;
        public double betaEgress_Time=0.0;
        public double betaParkingTime_u_min = 0.0;
        public double betaAge=0.0;
        public double betaBikeAcc=0.0;
        public double betaCarAcc=0.0;
        public double betaPTPass=0.0;
        public double sigma=0.0;

    }

    public class WalkParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;

        public double betaAge=0.0;
        public double betaBikeAcc=0.0;
        public double betaCarAcc=0.0;
        public double betaPTPass=0.0;

        public double sigma=0.0;

    }

    public static class BikeShareParameters{
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double betaAccess_Time = 0.0;
        public double betaEgress_Time=0.0;
        public double betaParkingTime_u_min = 0.0;
        public double betaPedelec=0.0;
        public double betaAvailability=0.0;
        public double betaAge=0.0;
        public double betaBikeAcc=0.0;
        public double betaCarAcc=0.0;
        public double betaPTPass=0.0;

        public double sigma=0.0;

    }
    public static class EScooterParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
        public double betaAccess_Time = 0.0;
        public double betaEgress_Time=0.0;
        public double betaParkingTime_u_min = 0.0;

        public double betaBattery=0.0;
        public double betaAge=0.0;
        public double betaBikeAcc=0.0;
        public double betaCarAcc=0.0;
        public double betaPTPass=0.0;

        public double sigma=0.0;

    }






    public double sigmaCost=0.0;

    public double betaCost_u_MU = 0.0;

    public final CarParameters car = new CarParameters();
    public final PtParameters pt = new PtParameters();
    public final BikeParameters bike = new BikeParameters();
    public final WalkParameters walk = new WalkParameters();
    public final EScooterParameters eScooter = new EScooterParameters();
    public final BikeShareParameters bikeShare = new BikeShareParameters();


    public static SMMParameters buildDefault() {
        // This is a copy & paste

        SMMParameters parameters = new SMMParameters();
      
        // Cost
        parameters.betaCost_u_MU = -1.886;
        parameters.sigmaCost=-1.414;


        // Car
        parameters.car.accessTime=2*60;
        parameters.car.egressTime=5*60;
        parameters.car.parkingTime=4*60;
        parameters.car.betaTravelTime_u_min=-0.057;
        parameters.car.betaAccess_Time=-0.034;
        parameters.car.betaEgress_Time=-0.042;
        parameters.car.betaParkingTime_u_min=-0.040;
        parameters.car.sigma=1;
        parameters.car.pool=0.776;

        // PT
        parameters.pt.betaTravelTime_u_min = -0.065;
        parameters.pt.betaAccess_Time=-0.040;
        parameters.pt.betaEgress_Time=-0.03;
        parameters.pt.betaTransfers=-0.298;
        parameters.pt.alpha_u=-1.205;
        parameters.pt.betaAge=0.013;
        parameters.pt.betaBikeAcc=0.9;
        parameters.pt.betaCarAcc=-1.556;
        parameters.pt.betaPTPass=3.651;
        parameters.pt.sigma=1.792;

        // Walk

        parameters.walk.alpha_u=4.551;
        parameters.walk.betaAge=0.020;
        parameters.walk.betaBikeAcc=0.495;
        parameters.walk.betaCarAcc=-1.216;
        parameters.pt.betaPTPass=1.64;
        parameters.walk.sigma=-2.933;
        parameters.walk.betaTravelTime_u_min = -0.212;

        // Bike
        // This is assumed due to similarities with BikeSharing
        parameters.bike.betaTravelTime_u_min = -0.09;
        parameters.bike.accessTime=1*60;
        parameters.bike.egressTime=1*60;
        parameters.bike.betaAccess_Time=-0.040;
        parameters.bike.betaEgress_Time=-0.03;
        parameters.bike.betaParkingTime_u_min=-0.040;
        parameters.bike.alpha_u=-1.199;
        parameters.bike.betaAge=-0.041;
        parameters.bike.betaBikeAcc=2.345;
        parameters.bike.betaCarAcc=-1.026;
        parameters.bike.betaPTPass=1.799;

        parameters.bike.sigma=1.587;
        // Walk


        // BikeShare
        parameters.bikeShare.betaTravelTime_u_min = -0.09;
        parameters.bikeShare.betaAccess_Time=-0.040;
        parameters.bikeShare.betaEgress_Time=-0.03;
        parameters.bikeShare.betaParkingTime_u_min=-0.040;
        parameters.bikeShare.betaAvailability=0.006;
        parameters.bikeShare.alpha_u=-1.199;
        parameters.bikeShare.betaAge=-0.041;
        parameters.bikeShare.betaBikeAcc=2.345;
        parameters.bikeShare.betaCarAcc=-1.026;
        parameters.bikeShare.betaPTPass=1.799;

        parameters.bikeShare.sigma=1.587;

        // EScooter
        parameters.eScooter.betaTravelTime_u_min = -0.116;
        parameters.eScooter.betaAccess_Time=-0.040;
        parameters.eScooter.betaEgress_Time=-0.03;
        parameters.eScooter.betaParkingTime_u_min=-0.040;
        parameters.eScooter.betaBattery=0.006;
        parameters.eScooter.alpha_u=-1.574;
        parameters.eScooter.betaAge=-0.051;
        parameters.eScooter.betaBikeAcc=1.811;
        parameters.eScooter.betaCarAcc=-0.858;
        parameters.eScooter.betaPTPass=1.621;
        parameters.eScooter.sigma=1.315;

       

        return parameters;
    }
}



