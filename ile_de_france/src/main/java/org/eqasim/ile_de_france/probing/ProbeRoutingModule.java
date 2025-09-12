package org.eqasim.ile_de_france.probing;

import java.util.Set;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.RoutingModule;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class ProbeRoutingModule extends AbstractModule {
    @Override
    public void install() {
        ProbeConfigGroup config = ProbeConfigGroup.get(getConfig());

        if (config.useProbeTravelTimes) {
            for (String mode : Set.of("car", "car_passenger")) {
                // delegate
                bind(Key.get(RoutingModule.class, Names.named("base_" + mode)))
                        .toProvider(new NetworkRoutingProvider(mode));

                // override
                addRoutingModuleBinding(mode).toProvider(new Provider<>() {
                    @Inject
                    Injector injector;

                    @Override
                    public ProbeNetworkRoutingModule get() {
                        RoutingModule delegate = injector
                                .getInstance(Key.get(RoutingModule.class, Names.named("base_" + mode)));
                        return new ProbeNetworkRoutingModule(delegate, mode);
                    }
                });
            }
        }
    }
}
