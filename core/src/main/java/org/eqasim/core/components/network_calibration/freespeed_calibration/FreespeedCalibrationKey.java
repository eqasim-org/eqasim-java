package org.eqasim.core.components.network_calibration.freespeed_calibration;

import java.util.Objects;

public final class FreespeedCalibrationKey {
    private final int category;
    private final String municipalityType;
    private final int specialRegion;

    public FreespeedCalibrationKey(int category, String municipalityType, int specialRegion) {
        this.category = category;
        this.municipalityType = municipalityType == null ? "unknown" : municipalityType;
        this.specialRegion = Math.max(0, specialRegion);
    }

    public int getCategory() {
        return category;
    }

    public String getMunicipalityType() {
        return municipalityType;
    }

    public int getSpecialRegion() {
        return specialRegion;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FreespeedCalibrationKey that)) {
            return false;
        }
        return category == that.category
                && specialRegion == that.specialRegion
                && municipalityType.equals(that.municipalityType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, municipalityType, specialRegion);
    }

    @Override
    public String toString() {
        return category + "_" + municipalityType + "_" + specialRegion;
    }
}
