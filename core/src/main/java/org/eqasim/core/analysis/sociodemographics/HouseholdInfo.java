package org.eqasim.core.analysis.sociodemographics;

import org.matsim.households.Household;

import java.util.Collection;

public class HouseholdInfo {
    public Collection<String> householdAttributes;
    public Collection<Household> households;

    public HouseholdInfo(Collection<String> householdAttributes, Collection<Household> households) {
        this.householdAttributes = householdAttributes;
        this.households = households;
    }
}
