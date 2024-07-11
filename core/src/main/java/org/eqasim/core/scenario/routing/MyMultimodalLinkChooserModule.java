package org.eqasim.core.scenario.routing;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MultimodalLinkChooser;

/**
 * Binding ParkingLinkChooser to MultimodalLinkChooser
 * 
 * @author akramelb
 */
public class MyMultimodalLinkChooserModule extends AbstractModule {

    @Override
    public void install() {
        bind(MultimodalLinkChooser.class).to(ParkingLinkChooser.class);
    }
}
