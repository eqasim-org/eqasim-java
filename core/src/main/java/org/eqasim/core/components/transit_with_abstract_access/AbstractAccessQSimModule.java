package org.eqasim.core.components.transit_with_abstract_access;

import org.eqasim.core.components.transit_with_abstract_access.analysis.AbstractAccessLegListener;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;

public class AbstractAccessQSimModule extends AbstractQSimModule {

    public static final String COMPONENT_NAME = "AbstractAccess";

    @Override
    protected void configureQSim() {
        if(getConfig().getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME) != null) {
            addQSimComponentBinding(COMPONENT_NAME).to(AbstractAccessDepartureEventCreator.class);
            addMobsimScopeEventHandlerBinding().to(AbstractAccessLegListener.class);
        }
    }

    static public void configure(QSimComponentsConfig components, Config config) {
        if(config.getModules().get(AbstractAccessModuleConfigGroup.ABSTRACT_ACCESS_GROUP_NAME) != null) {
            components.addNamedComponent(COMPONENT_NAME);
        }
    }
}
