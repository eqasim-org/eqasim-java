package org.eqasim.ile_de_france.mode_choice.parameters;

public class IDFDrtCostParameters extends IDFCostParameters {
    public static final double DEFAULT_DRT_COST_EUR_KM = 0.15;
    public double drtCost_EUR_km;

    public static IDFDrtCostParameters buildDefault() {
        return fromIDFCostParameters(IDFCostParameters.buildDefault(), DEFAULT_DRT_COST_EUR_KM);
    }

    public static IDFDrtCostParameters fromIDFCostParameters(IDFCostParameters idfCostParameters, double drtCost_EUR_km) {
        IDFDrtCostParameters drtCostParameters = new IDFDrtCostParameters();
        drtCostParameters.carCost_EUR_km = idfCostParameters.carCost_EUR_km;
        drtCostParameters.drtCost_EUR_km = drtCost_EUR_km;
        return drtCostParameters;
    }
}
