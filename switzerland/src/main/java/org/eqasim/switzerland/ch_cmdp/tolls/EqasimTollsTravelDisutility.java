package org.eqasim.switzerland.ch_cmdp.tolls;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class EqasimTollsTravelDisutility implements TravelDisutility {
    private final TravelDisutility delegate;
    private final Tolls tolls;
    private final MarginalCostOfTolls marginalCostOfTolls;

    public EqasimTollsTravelDisutility(TravelDisutility delegate, Tolls tolls, MarginalCostOfTolls marginalCostOfTolls) {
        this.delegate = delegate;
        this.tolls = tolls;
        this.marginalCostOfTolls = marginalCostOfTolls;
    }

    @Override
    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
        double disutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);
        double linkCost = tolls.getToll(link, vehicle);
        if (linkCost>0){
            return disutility+linkCost * marginalCostOfTolls.getMarginalCostOfTolls(vehicle);
        }
        return disutility;
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        return delegate.getLinkMinimumTravelDisutility(link);
    }
}
