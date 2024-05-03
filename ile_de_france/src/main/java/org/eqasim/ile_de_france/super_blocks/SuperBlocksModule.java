package org.eqasim.ile_de_france.super_blocks;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.ile_de_france.super_blocks.defs.SuperBlocksLogic;
import org.eqasim.ile_de_france.super_blocks.permissions.ActivityTypeBasedSuperBlockPermission;
import org.eqasim.ile_de_france.super_blocks.permissions.SuperBlockPermission;
import org.eqasim.ile_de_france.super_blocks.routing.SuperBlocksTravelDisutility;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import java.io.IOException;
import java.util.List;

public class SuperBlocksModule extends AbstractEqasimExtension {
    @Override
    protected void installEqasimExtension() {
        addTravelDisutilityFactoryBinding("car").to(SuperBlocksTravelDisutility.Factory.class);
        bind(SuperBlockPermission.class).to(ActivityTypeBasedSuperBlockPermission.class);
    }

    @Provides
    public ActivityTypeBasedSuperBlockPermission provideActivityTypeBasedSuperBlockPermission() {
        return new ActivityTypeBasedSuperBlockPermission(List.of("home", "work"));
    }

    @Provides
    @Singleton
    public SuperBlocksLogic provideSuperBlocksLogic(Population population, Network network, SuperBlockPermission superBlockPermission) throws IOException {
        String superBlocksShapefilePath = "F:\\scenarios\\idf\\misc\\superblocks\\Paris-superblocks_epsg2154.shp";
        return new SuperBlocksLogic(superBlocksShapefilePath, population, network, superBlockPermission);
    }

    @Provides
    @Singleton
    public SuperBlocksTravelDisutility.Factory providerSuperBlocksTravelDisutilityFactory(Config config, SuperBlocksLogic superBlocksLogic) {
        return new SuperBlocksTravelDisutility.Factory(new RandomizingTimeDistanceTravelDisutilityFactory("car", config), superBlocksLogic);
    }
}
