package org.eqasim.switzerland.ch_cmdp.walk;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.config.ConfigUtils;

/** Install as an overriding module. Use new WalkModule(MyExpression.class) for a custom expression. */
public final class WalkModule extends AbstractModule {
    private final Logger logger = LogManager.getLogger(WalkModule.class);
    private final Class<? extends WalkTravelTime> expression;

    public WalkModule() { this.expression = null; }
    public WalkModule(Class<? extends WalkTravelTime> expression) { this.expression = expression; }

    @Override public void install() {
        var settings = ConfigUtils.addOrGetModule(getConfig(), WalkConfigGroup.class);
        Class<? extends WalkTravelTime> selected = expression != null ? expression
                : "mobis".equals(settings.getModel()) ? MobisWalkTravelTime.class : TeleportationWalkTravelTime.class;

        if (getConfig().routing().getNetworkModes().contains(TransportMode.walk)
                || getConfig().routing().getOrCreateModeRoutingParams(TransportMode.walk).getTeleportedModeFreespeedFactor() != null)
            throw new IllegalArgumentException("WalkModule requires teleported walking with a configured speed");

        if (selected == MobisWalkTravelTime.class) {
            bind(WalkTravelTime.class).toInstance(new MobisWalkTravelTime(settings.getV0MetersPerSecond(),
                    settings.getK(), settings.getUphillPenalty(), settings.getFlatGrade(), settings.getDetourFactor()));
        } else {
            bind(WalkTravelTime.class).to(selected).in(Singleton.class);
        }
        bind(WalkTravelTimeCalculator.class).in(Singleton.class);
        addRoutingModuleBinding(TransportMode.walk).toProvider(WalkProvider.class);

        // A link's fixed-time access bypasses the walk router. Custom expressions need geometric access.
        if (selected != TeleportationWalkTravelTime.class
                && getConfig().routing().getAccessEgressType() == AccessEgressType.walkConstantTimeToLink) {
            getConfig().routing().setAccessEgressType(AccessEgressType.accessEgressModeToLink);
            logger.info("Custom walking expression: using geometric network access/egress instead of fixed link times.");
        }
    }

    static final class WalkProvider implements Provider<RoutingModule> {
        @Inject Scenario scenario;
        @Inject WalkTravelTimeCalculator calculator;
        @Override public RoutingModule get() {
            var params = scenario.getConfig().routing().getOrCreateModeRoutingParams(TransportMode.walk);
            return new WalkRoutingModule(DefaultRoutingModules.createTeleportationRouter(TransportMode.walk, scenario, params), calculator);
        }
    }
}
