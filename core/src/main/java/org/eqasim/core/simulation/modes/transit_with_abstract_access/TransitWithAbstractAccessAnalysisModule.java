package org.eqasim.core.simulation.modes.transit_with_abstract_access;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccesses;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.analysis.AbstractAccessAnalysisOutputListener;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.analysis.AbstractAccessLegListener;

public class TransitWithAbstractAccessAnalysisModule extends AbstractEqasimExtension {
    @Override
    protected void installEqasimExtension() {
        bind(AbstractAccessLegListener.class).toProvider(new Provider<>() {

            @Inject
            private AbstractAccesses abstractAccesses;
            @Override
            public AbstractAccessLegListener get() {
                return new AbstractAccessLegListener(abstractAccesses);
            }
        }).asEagerSingleton();
        addControlerListenerBinding().to(AbstractAccessAnalysisOutputListener.class);
    }
}
