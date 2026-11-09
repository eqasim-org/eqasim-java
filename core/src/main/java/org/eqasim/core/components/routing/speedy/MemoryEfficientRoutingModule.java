package org.eqasim.core.components.routing.speedy;

import com.google.inject.Singleton;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.groups.ControllerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

/** Replacement for the configured SpeedyALT router; the CLI switch defaults to enabled. */
public final class MemoryEfficientRoutingModule extends AbstractModule {
    public static final String OPTION = "memory-efficient-routing";

    public static boolean isEnabled(CommandLine command) throws ConfigurationException {
        String value = command.getOption(OPTION).orElse("true");
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new ConfigurationException("--" + OPTION + " must be true or false: " + value);
    }

    @Override public void install() {
        if (getConfig().controller().getRoutingAlgorithmType() != RoutingAlgorithmType.SpeedyALT) {
            throw new IllegalArgumentException("Memory-efficient routing requires controller.routingAlgorithmType=SpeedyALT");
        }
        bind(LeastCostPathCalculatorFactory.class).to(MemoryEfficientSpeedyALTFactory.class).in(Singleton.class);
        org.apache.logging.log4j.LogManager.getLogger(MemoryEfficientRoutingModule.class)
                .info("Memory-efficient SpeedyALT enabled; restrictions and existing travel times/costs are retained.");
    }
}
