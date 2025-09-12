package org.eqasim.ile_de_france.probing;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class ProbeConfigGroup extends ReflectiveConfigGroup {
    static public final String NAME = "eqasim:probe";

    @Parameter
    public boolean useProbeTravelTimes = false;

    public ProbeConfigGroup() {
        super(NAME);
    }

    static public ProbeConfigGroup get(Config config) {
        return (ProbeConfigGroup) config.getModules().get(NAME);
    }
}
