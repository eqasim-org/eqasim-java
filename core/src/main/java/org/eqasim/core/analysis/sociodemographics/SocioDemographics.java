package org.eqasim.core.analysis.sociodemographics;

import org.matsim.api.core.v01.population.Person;

import java.util.Collection;

public class SocioDemographics {
    public Collection<String> populationAttributes;
    public Collection<? extends Person> population;

    public SocioDemographics(Collection<String> populationAttributes, Collection<? extends Person> population) {
        this.populationAttributes = populationAttributes;
        this.population = population;
    }
}
