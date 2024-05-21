package org.eqasim.ile_de_france.super_blocks.defs;

import org.eqasim.ile_de_france.super_blocks.permissions.SuperBlockPermission;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SuperBlocksLogic {
    private final IdMap<Person, IdSet<SuperBlock>> superBlocksByPerson = new IdMap<>(Person.class);
    private final IdMap<Link, Id<SuperBlock>> superBlockByLink = new IdMap<>(Link.class);
    private final IdMap<Link, IdSet<SuperBlock>> borderingSuperBlockByLink = new IdMap<>(Link.class);

    public SuperBlocksLogic(String filePath, Population population, Network network, SuperBlockPermission superBlockPermission) throws IOException {
        IdMap<SuperBlock, SuperBlock> superBlocks = SuperBlock.readFromShapefile(filePath);
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
            if(!link.getAllowedModes().contains("car")) {
                continue;
            }
            List<Id<SuperBlock>> superBlockIdList = new ArrayList<>();
            superBlocks.values().stream()
                    .filter(superBlock -> superBlock.intersectsWithLink(link))
                    .map(SuperBlock::getId)
                    .forEach(superBlockIdList::add);
            if(superBlockIdList.size() > 1) {
                throw new IllegalStateException(String.format("Link %s in more than one superblock: %s", link.getId().toString(), String.join(",", superBlockIdList.stream().map(Id::toString).toList())));
            }
            if(superBlockIdList.size() > 0) {
                superBlockByLink.put(link.getId(), superBlockIdList.get(0));
            }

            IdSet<SuperBlock> borderingSuperblocksIdSet = new IdSet<>(SuperBlock.class);
            superBlocks.values().stream()
                    .filter(superBlock -> superBlock.isLinkAtFrontier(link))
                    .map(SuperBlock::getId)
                    .forEach(borderingSuperblocksIdSet::add);
            if(borderingSuperblocksIdSet.size() > 0) {
                borderingSuperBlockByLink.put(link.getId(), borderingSuperblocksIdSet);
            }
        }
    }

    public IdMap<Person, IdSet<SuperBlock>> getSuperBlocksByPerson() {
        return superBlocksByPerson;
    }

    public IdMap<Link, Id<SuperBlock>> getSuperBlockByLink() {
        return superBlockByLink;
    }

    public IdMap<Link, IdSet<SuperBlock>> getBorderingSuperBlockByLink() {
        return borderingSuperBlockByLink;
    }
}
