package org.eqasim.core.components.network_calibration.freespeed_calibration;

import java.util.Objects;

public class LinkGroupKey {
    private final int category;
    private final String municipalityType;

    public LinkGroupKey(int category, String municipalityType) {
        this.category = category;
        this.municipalityType = municipalityType == null ? "unknown" : municipalityType;
    }

    public int getCategory() {
        return category;
    }

    public String getMunicipalityType() {
        return municipalityType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LinkGroupKey that)) {
            return false;
        }
        return category == that.category && municipalityType.equals(that.municipalityType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, municipalityType);
    }

    public String toString() {
        return String.valueOf(category) + "_" + municipalityType;
    }
}

