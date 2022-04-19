package org.eqasim.examples.corsica_drt.mode_choice.parameters;

import org.eqasim.core.simulation.mode_choice.ParameterDefinition;

public class KraussCostParameters implements ParameterDefinition {
    public double carCost_Km;
    public double bookingCostBikeShare;
    public double bikeShareCost_Km;
    public double bookingCostEScooter;
    public double eScooterCost_km;
    public double pTTicketCost;

    public  static KraussCostParameters buildDefault(){
        KraussCostParameters parameters = new KraussCostParameters();

        parameters.carCost_Km= 0.30;
        parameters.bookingCostBikeShare = 0.25;
        parameters.bikeShareCost_Km=1;
        parameters.bookingCostEScooter = 0.5;
        parameters.eScooterCost_km=0.5;
        parameters.pTTicketCost=3;

        return parameters;
    }

}
