package org.eqasim.core.simulation.modes.transit_with_abstract_access;

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
import org.eqasim.core.simulation.modes.transit_with_abstract_access.routing.TransitWithAbstractAccessRoutingModule;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.net.URL;

public class TransitWithAbstractAccessModule extends AbstractEqasimExtension {

    @Inject
    private TransitWithAbstractAbstractAccessModuleConfigGroup configGroup;


    @Override
    protected void installEqasimExtension() {
        addRoutingModuleBinding(this.configGroup.getModeName()).to(TransitWithAbstractAccessRoutingModule.class).asEagerSingleton();
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
