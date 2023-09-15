package org.eqasim.core.components.transit_with_abstract_access;

import com.google.inject.*;
import com.google.inject.name.Named;
import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccesses;
import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccessesFileReader;
import org.eqasim.core.components.transit_with_abstract_access.analysis.AbstractAccessAnalysisOutputListener;
import org.eqasim.core.components.transit_with_abstract_access.analysis.AbstractAccessLegListener;
import org.eqasim.core.components.transit_with_abstract_access.routing.TransitWithAbstractAccessRoutingModule;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.utilities.UtilityEstimator;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.net.URL;
import java.util.Map;

public class AbstractAccessModule extends AbstractEqasimExtension {

    private final AbstractAccessModuleConfigGroup configGroup;

    public AbstractAccessModule(AbstractAccessModuleConfigGroup configGroup) {
        this.configGroup = configGroup;
    }

    @Override
    protected void installEqasimExtension() {
        addRoutingModuleBinding(this.configGroup.getModeName()).to(TransitWithAbstractAccessRoutingModule.class);
        bindUtilityEstimator(this.configGroup.getModeName()).toProvider(new Provider<UtilityEstimator>() {
            @Inject
            Map<String, Provider<UtilityEstimator>> utilityEstimators;

            @Override
            public UtilityEstimator get() {
                return utilityEstimators.get(TransportMode.pt).get();
            }
        });
        bind(AbstractAccessLegListener.class).toProvider(new Provider<AbstractAccessLegListener>() {

            @Inject
            private AbstractAccesses abstractAccesses;
            @Override
            public AbstractAccessLegListener get() {
                return new AbstractAccessLegListener(abstractAccesses);
            }
        }).asEagerSingleton();
        addControlerListenerBinding().to(AbstractAccessAnalysisOutputListener.class);
    }

    @Provides
    public TransitWithAbstractAccessRoutingModule provideTransitWithAbstractAccessRoutingModule(TransitSchedule transitSchedule, @Named("pt") RoutingModule ptRoutingModule, Network network, PopulationFactory populationFactory, AbstractAccesses abstractAccesses) {
        return new TransitWithAbstractAccessRoutingModule(transitSchedule, abstractAccesses, network, ptRoutingModule, populationFactory);
    }

    @Provides
    @Singleton
    public AbstractAccesses provideAbstractAccesses(TransitSchedule transitSchedule) {
        AbstractAccessesFileReader fileReader = new AbstractAccessesFileReader(transitSchedule);
        URL inputAccessFilePath = ConfigGroup.getInputFileURL(getConfig().getContext(), this.configGroup.getAccessItemsFilePath());
        fileReader.readFile(inputAccessFilePath.getPath());
        return new AbstractAccesses(fileReader.getAccessItems());
    }
}
