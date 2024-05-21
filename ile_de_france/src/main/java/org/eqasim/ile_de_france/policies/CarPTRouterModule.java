package org.eqasim.ile_de_france.policies;

import org.matsim.core.controler.AbstractModule;

public class CarPTRouterModule extends AbstractModule{

    @Override
    public void install() {
        addRoutingModuleBinding("car_pt").toProvider(CarPTRouterProvider.class);
    }

}
