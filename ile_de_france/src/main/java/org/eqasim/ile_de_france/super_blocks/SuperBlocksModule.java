package org.eqasim.ile_de_france.super_blocks;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.eqasim.core.scenario.routing.PopulationRouterModule;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.ile_de_france.super_blocks.defs.SuperBlocksLogic;
import org.eqasim.ile_de_france.super_blocks.handlers.SuperblockStartupListener;
import org.eqasim.ile_de_france.super_blocks.handlers.SuperblockViolationHandler;
import org.eqasim.ile_de_france.super_blocks.permissions.ActivityTypeBasedSuperBlockPermission;
import org.eqasim.ile_de_france.super_blocks.permissions.SuperBlockPermission;
import org.eqasim.ile_de_france.super_blocks.routing.SuperBlocksTravelDisutility;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class SuperBlocksModule extends AbstractEqasimExtension {
	private final String superblocksPath;
	
	public SuperBlocksModule(String superblocksPath) {
		this.superblocksPath = superblocksPath;
	}
	
    @Override
    protected void installEqasimExtension() {
        addTravelDisutilityFactoryBinding("car").to(SuperBlocksTravelDisutility.Factory.class);
        bind(SuperBlockPermission.class).to(ActivityTypeBasedSuperBlockPermission.class);
        addEventHandlerBinding().to(SuperblockViolationHandler.class).asEagerSingleton();
        addControlerListenerBinding().to(SuperblockViolationHandler.class).asEagerSingleton();
        install(new PopulationRouterModule(getConfig().global().getNumberOfThreads(), 100, true, Set.of("car")));
        addControlerListenerBinding().to(SuperblockStartupListener.class);
    }

    @Provides
    public ActivityTypeBasedSuperBlockPermission provideActivityTypeBasedSuperBlockPermission() {
        return new ActivityTypeBasedSuperBlockPermission(List.of("home", "work"));
    }

    @Provides
    @Singleton
    public SuperBlocksLogic provideSuperBlocksLogic(Population population, Network network, SuperBlockPermission superBlockPermission) throws IOException {
        return new SuperBlocksLogic(superblocksPath, population, network, superBlockPermission);
    }

    @Provides
    @Singleton
    public SuperBlocksTravelDisutility.Factory providerSuperBlocksTravelDisutilityFactory(Config config, SuperBlocksLogic superBlocksLogic) {
        return new SuperBlocksTravelDisutility.Factory(new RandomizingTimeDistanceTravelDisutilityFactory("car", config), superBlocksLogic);
    }
}
