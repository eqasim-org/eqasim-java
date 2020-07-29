package org.eqasim.core.analysis.sociodemographics;

import org.matsim.households.Household;
import org.matsim.households.Households;

import java.util.Collection;
import java.util.HashSet;

public class HouseholdsReader {

    public HouseholdInfo read(Households households) {

        Collection<String> householdAttributes = new HashSet<>();

        for (Household household : households.getHouseholds().values()) {
            householdAttributes.addAll(household.getAttributes().getAsMap().keySet());
        }

        return new HouseholdInfo(householdAttributes, households.getHouseholds().values());
    }
}