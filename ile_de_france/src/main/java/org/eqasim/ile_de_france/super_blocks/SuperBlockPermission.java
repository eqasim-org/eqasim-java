package org.eqasim.ile_de_france.super_blocks;

import org.matsim.api.core.v01.population.Person;

public interface SuperBlockPermission {
    boolean isPersonAllowedInSuperBlock(Person person, SuperBlock superBlock);
}
