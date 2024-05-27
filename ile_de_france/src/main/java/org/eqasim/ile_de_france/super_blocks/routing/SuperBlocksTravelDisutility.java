package org.eqasim.ile_de_france.super_blocks.routing;

import com.google.inject.Inject;
import org.eqasim.ile_de_france.super_blocks.defs.SuperBlock;
import org.eqasim.ile_de_france.super_blocks.defs.SuperBlocksLogic;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class SuperBlocksTravelDisutility implements TravelDisutility {
    public static final double WITHIN_SUPERBLOCK_DISUTILITY_FACTOR = 10e6;
    private final IdMap<Person, IdSet<SuperBlock>> superBlocksByPerson;
    private final IdMap<Link, Id<SuperBlock>> superBlockByLink;
    private final IdMap<Link, IdSet<SuperBlock>> borderingSuperBlockByLink;
    private final TravelDisutility delegate;

    public SuperBlocksTravelDisutility(IdMap<Person, IdSet<SuperBlock>> superBlocksByPerson, IdMap<Link, Id<SuperBlock>> superBlockByLink, IdMap<Link, IdSet<SuperBlock>> borderingSuperBlockByLink, TravelDisutility delegate) {
        this.superBlocksByPerson = superBlocksByPerson;
        this.superBlockByLink = superBlockByLink;
        this.borderingSuperBlockByLink = borderingSuperBlockByLink;
        this.delegate = delegate;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        IdSet<SuperBlock> superBlocksIds = this.borderingSuperBlockByLink.get(link.getId());
        double disutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);
        if(superBlocksIds != null) {
            IdSet<SuperBlock> personSuperBlocks = this.superBlocksByPerson.get(person.getId());
            if(personSuperBlocks == null || superBlocksIds.stream().anyMatch(id -> !personSuperBlocks.contains(id))) {
                disutility *= WITHIN_SUPERBLOCK_DISUTILITY_FACTOR;
            }
        }
        return disutility;
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return delegate.getLinkMinimumTravelDisutility(link);
    }

    public static class Factory implements TravelDisutilityFactory {

        private final TravelDisutilityFactory delegateFactory;
        private final IdMap<Person, IdSet<SuperBlock>> superBlocksByPerson;
        private final IdMap<Link, Id<SuperBlock>> superBlockByLink;
        private final IdMap<Link, IdSet<SuperBlock>> borderingSuperBlockByLink;

        public Factory(TravelDisutilityFactory delegateFactory, SuperBlocksLogic superBlocksLogic) {
            this(delegateFactory, superBlocksLogic.getSuperBlocksByPerson(), superBlocksLogic.getSuperBlockByLink(), superBlocksLogic.getBorderingSuperBlockByLink());
        }

        public Factory(TravelDisutilityFactory delegateFactory, IdMap<Person, IdSet<SuperBlock>> superBlocksByPerson, IdMap<Link, Id<SuperBlock>> superBlockByLink, IdMap<Link, IdSet<SuperBlock>> borderingSuperBlockByLink) {
            this.delegateFactory = delegateFactory;
            this.superBlocksByPerson = superBlocksByPerson;
            this.superBlockByLink = superBlockByLink;
            this.borderingSuperBlockByLink = borderingSuperBlockByLink;
        }

        @Override
        @Inject
        public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
            return new SuperBlocksTravelDisutility(superBlocksByPerson, superBlockByLink, borderingSuperBlockByLink, delegateFactory.createTravelDisutility(timeCalculator));
        }
    }
}
