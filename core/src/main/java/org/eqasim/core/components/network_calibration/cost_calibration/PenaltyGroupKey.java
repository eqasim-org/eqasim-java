package org.eqasim.core.components.network_calibration.cost_calibration;

/**
 * Group key for penalties calibration and lookup.
 */
public final class PenaltyGroupKey {
    private final int linkCategory;
    private final boolean urban;
    private final int specialRegion;

    public PenaltyGroupKey(int linkCategory, boolean urban, int specialRegion) {
        if (linkCategory < 1 || linkCategory > 5) {
            throw new IllegalArgumentException("linkCategory must be between 1 and 5.");
        }
        if (specialRegion < 0) {
            throw new IllegalArgumentException("specialRegion must be >= 0.");
        }

        this.linkCategory = linkCategory;
        this.urban = urban;
        this.specialRegion = specialRegion;
    }

    public int getLinkCategory() {
        return linkCategory;
    }

    public boolean isUrban() {
        return urban;
    }

    public int getSpecialRegion() {
        return specialRegion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PenaltyGroupKey that = (PenaltyGroupKey) o;
        return linkCategory == that.linkCategory
                && urban == that.urban
                && specialRegion == that.specialRegion;
    }

    @Override
    public int hashCode() {
        // Exactly Objects.hash's 31-fold integer result, without its varargs
        // array or primitive boxing on each routing lookup. Preserve overflow.
        int hash = 31 + linkCategory;
        hash = 31 * hash + Boolean.hashCode(urban);
        return 31 * hash + specialRegion;
    }

    @Override
    public String toString() {
        return linkCategory + "_" + (urban ? "urban" : "rural") + "_" + specialRegion;
    }
}
