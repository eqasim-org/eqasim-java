package org.eqasim.core.analysis.sociodemographics;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.Collection;
import java.util.HashSet;

public class SocioDemographicsReader {

    public SocioDemographics read(Population population) {

        Collection<String> populationAttributes = new HashSet<>();

        for (Person person : population.getPersons().values()) {
            populationAttributes.addAll(person.getAttributes().getAsMap().keySet());
        }

        return new SocioDemographics(populationAttributes, population.getPersons().values());
    }
}