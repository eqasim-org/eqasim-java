package org.eqasim.ile_de_france.trb2020;

import org.eqasim.ile_de_france.mode_choice.utilities.variables.BikeVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.CarVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.PtVariables;
import org.eqasim.ile_de_france.mode_choice.utilities.variables.WalkVariables;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class UtilityShareItem {
    public Id<Person> personId;
    public CarVariables carVariables;
    public PtVariables ptVariables;
    public BikeVariables bikeVariables;
    public WalkVariables walkVariables;

    public UtilityShareItem(Id<Person> personId) {
        this.personId = personId;
    }

    public void setCarVariables(CarVariables carVariables) {
        this.carVariables = carVariables;
    }

    public void setPtVariables(PtVariables ptVariables) {
        this.ptVariables = ptVariables;
    }

    public void setBikeVariables(BikeVariables bikeVariables) {
        this.bikeVariables = bikeVariables;
    }

    public void setWalkVariables(WalkVariables walkVariables) {
        this.walkVariables = walkVariables;
    }
}
