package org.eqasim.core.simulation.modes.transit_with_abstract_access;

import ch.sbb.matsim.routing.pt.raptor.RaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccesses;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.abstract_access.AbstractAccessesFileReader;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.analysis.AbstractAccessAnalysisOutputListener;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.analysis.AbstractAccessLegListener;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.TransitWithAbstractAccessData;
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.TransitWithAbstractAccessRoutingModule;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.net.URL;

public class TransitWithAbstractAccessModule extends AbstractEqasimExtension {

    @Inject
    private TransitWithAbstractAbstractAccessModuleConfigGroup configGroup;


    @Override
    protected void installEqasimExtension() {
        addRoutingModuleBinding(this.configGroup.getModeName()).to(TransitWithAbstractAccessRoutingModule.class);
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

    @Provides
    public TransitWithAbstractAccessRoutingModule provideTransitWithAbstractAccessRoutingModule(TransitWithAbstractAccessData transitWithAbstractAccessData, @Named("pt") RoutingModule ptRoutingModule, PopulationFactory populationFactory, AbstractAccesses abstractAccesses, RaptorParametersForPerson raptorParametersForPerson, Config config) {
        return new TransitWithAbstractAccessRoutingModule(transitWithAbstractAccessData, abstractAccesses, ptRoutingModule, populationFactory, raptorParametersForPerson, RaptorUtils.createStaticConfig(config));
    }

    @Provides
    @Singleton
    public AbstractAccesses provideAbstractAccesses(TransitSchedule transitSchedule) {
        AbstractAccessesFileReader fileReader = new AbstractAccessesFileReader(transitSchedule);
        URL inputAccessFilePath = ConfigGroup.getInputFileURL(getConfig().getContext(), this.configGroup.getAccessItemsFilePath());
        fileReader.readFile(inputAccessFilePath.getPath());
        return new AbstractAccesses(fileReader.getAccessItems());
    }

    @Provides
    @Singleton
    public TransitWithAbstractAccessData provideTransitWithAbstractAccessData(TransitSchedule transitSchedule, Network network) {
        return new TransitWithAbstractAccessData(transitSchedule, network);
    }
}
