package org.eqasim.core.simulation.modes.transit_with_abstract_access;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

public class TransitWithAbstractAccessQSimModule extends AbstractQSimModule {

    public static final String COMPONENT_NAME = "AbstractAccess";

    @Override
    protected void configureQSim() {
        if(getConfig().getModules().get(TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME) != null) {
            addQSimComponentBinding(COMPONENT_NAME).to(AbstractAccessDepartureEventCreator.class);
        }
    }

    static public void configure(QSimComponentsConfig components, Config config) {
        if(config.getModules().get(TransitWithAbstractAbstractAccessModuleConfigGroup.GROUP_NAME) != null) {
            components.addNamedComponent(COMPONENT_NAME);
        }
    }
}
