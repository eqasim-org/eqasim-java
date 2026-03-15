package org.eqasim.ile_de_france.vdf;

import org.eqasim.core.components.traffic.CrossingPenalty;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;

public class EqasimFixes {
    static public void install(Controller controller) {
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(CrossingPenalty.class).toInstance(link -> 0.0);
            }
        });
    }
}
