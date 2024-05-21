package org.eqasim.ile_de_france.policies;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MultimodalLinkChooser;


public class MyMultiModalLinkChooserModule extends AbstractModule {

    @Override
    public void install() {
        bind(MultimodalLinkChooser.class).to(ParkingLinkChooser.class);
    }
}
