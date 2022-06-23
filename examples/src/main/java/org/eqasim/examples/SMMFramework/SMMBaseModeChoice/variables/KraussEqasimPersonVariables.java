package org.eqasim.examples.SMMFramework.SMMBaseModeChoice.variables;

import org.eqasim.core.simulation.mode_choice.utilities.variables.PersonVariables;

public class KraussEqasimPersonVariables extends PersonVariables {
    private final  int bikeAcc;
    private final  int carAccessibility;
    private final int ptPass;
    private final int maasPass;

    public int getBikeAcc() {
        return bikeAcc;
    }

    public int getCarAccessibility() {
        return carAccessibility;
    }

    public int getMaasPass() {
        return maasPass;
    }

    Double randomCost;
    public KraussEqasimPersonVariables(int age_a, int bikeAcc, int carAccessibility, int ptPass, int maasPass) {
        super(age_a);
        this.bikeAcc = bikeAcc;
        this.carAccessibility = carAccessibility;
        this.ptPass = ptPass;
        this.maasPass = maasPass;

    }

    public int getPtPass() {
        return ptPass;
    }
}
