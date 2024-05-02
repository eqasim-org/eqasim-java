package org.eqasim.ile_de_france.super_blocks;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.ile_de_france.super_blocks.routing.SuperBlocksTravelDisutility;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SuperBlocksModule extends AbstractEqasimExtension {
    @Override
    protected void installEqasimExtension() {
        addTravelDisutilityFactoryBinding("car").to(SuperBlocksTravelDisutility.Factory.class);
        bind(SuperBlockPermission.class).to(ActivityTypeBasedSuperBlockPermission.class);
    }

    @Provides
    public ActivityTypeBasedSuperBlockPermission provideActivityTypeBasedSuperBlockPermission() {
        return new ActivityTypeBasedSuperBlockPermission(List.of("home", "work"));
    }

    @Provides
    @Singleton
    public SuperBlocksTravelDisutility.Factory providerSuperBlocksTravelDisutilityFactory(Config config, Network network, Population population, SuperBlockPermission superBlockPermission) throws IOException {
        String superBlocksShapefilePath = "C:\\Users\\tarek.chouaki\\simulations\\idf\\misc\\superblocks\\Paris-superblocks.shp";
        IdMap<SuperBlock, SuperBlock> superBlocks = SuperBlock.readFromShapefile(superBlocksShapefilePath);

        IdMap<Person, IdSet<SuperBlock>> superBlocksByPerson = new IdMap<>(Person.class);
        IdMap<Link, Id<SuperBlock>> superBlockByLink = new IdMap<>(Link.class);

        for(Person person: population.getPersons().values()) {
            IdSet<SuperBlock> superBlockIdSet = new IdSet<>(SuperBlock.class);
            superBlocks.values().stream()
                    .filter(superBlock -> superBlockPermission.isPersonAllowedInSuperBlock(person, superBlock))
                    .map(SuperBlock::getId)
                    .forEach(superBlockIdSet::add);
            if(superBlockIdSet.size() == 0) {
                superBlocksByPerson.put(person.getId(), superBlockIdSet);
            }
        }
        for(Link link: network.getLinks().values()) {
            List<Id<SuperBlock>> superBlockIdList = new ArrayList<>();
            superBlocks.values().stream()
                    .filter(superBlock -> superBlock.containsCoord(link.getFromNode().getCoord()) || superBlock.containsCoord(link.getToNode().getCoord()))
                    .map(SuperBlock::getId)
                    .forEach(superBlockIdList::add);
            if(superBlockIdList.size() > 1) {
                throw new IllegalStateException(String.format("Link %s in more than one superblock", link.getId().toString()));
            }
            if(superBlockIdList.size() > 0) {
                superBlockByLink.put(link.getId(), superBlockIdList.get(0));
            }
        }
        return new SuperBlocksTravelDisutility.Factory(new RandomizingTimeDistanceTravelDisutilityFactory("car", config), superBlocksByPerson, superBlockByLink);
    }
}
