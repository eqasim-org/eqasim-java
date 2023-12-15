package org.eqasim.core.simulation.modes.transit_access.teleported.teleported;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

public class AbstractAccessQSimModule extends AbstractQSimModule {

    public static final String COMPONENT_NAME = "AbstractAccess";

    @Override
    protected void configureQSim() {
        if(getConfig().getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME) != null) {
            addQSimComponentBinding(COMPONENT_NAME).to(AbstractAccessDepartureEventCreator.class);
        }
    }

    static public void configure(QSimComponentsConfig components, Config config) {
        if(config.getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME) != null) {
            components.addNamedComponent(COMPONENT_NAME);
        }
    }
}
