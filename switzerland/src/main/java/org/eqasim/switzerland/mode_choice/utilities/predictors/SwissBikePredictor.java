package org.eqasim.switzerland.mode_choice.utilities.predictors;

import java.util.List;

import com.google.inject.Inject;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.BikePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.switzerland.mode_choice.utilities.variables.SwissBikeVariables;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.population.routes.NetworkRoute;


public class SwissBikePredictor extends CachedVariablePredictor<SwissBikeVariables> {
    private final Scenario scenario;
    public final BikePredictor bikePredictor;

    @Inject
    public SwissBikePredictor(Scenario scenario, BikePredictor bikePredictor) {
        this.scenario = scenario;
        this.bikePredictor = bikePredictor;
    }

    @Override
    public SwissBikeVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
        if (elements.size() > 1) {
            throw new IllegalStateException("We do not support multi-stage bike trips yet.");
        }

        Leg leg = (Leg) elements.get(0);

        double S1L1 = 0;
        double S2L1 = 0;
        double S3L1 = 0;
        double S4L1 = 0;
        double S1L2 = 0;
        double S2L2 = 0;
        double S3L2 = 0;
        double S4L2 = 0;

        double routedDistance = leg.getRoute().getDistance();

        Network network = scenario.getNetwork();
        Id<Link> startLinkId = leg.getRoute().getStartLinkId();
        Id<Link> endLinkId = leg.getRoute().getEndLinkId();

        List<Id<Link>> linkIdList = ((NetworkRoute) leg.getRoute()).getLinkIds();// use (NetworkRoute) to use method getLinkIds
        linkIdList.add(startLinkId);
        linkIdList.add(endLinkId);
        for (Id<Link> linkId: linkIdList){
            Link link = network.getLinks().get(linkId);
            double numberLanes = link.getNumberOfLanes();
            double freespeed = link.getFreespeed();
            double linkLength = link.getLength();
            if (numberLanes ==1){
                if (freespeed <= 8.33333){ // <=30km/h
                    S1L1 += linkLength;
                }
                if ((freespeed > 8.33333)&(freespeed <= 13.8889)){ // <=50km/h
                    S2L1 += linkLength;
                }
                if ((freespeed > 13.8889)&(freespeed <= 16.6667)){ // <=60km/h
                    S3L1 += linkLength;
                }
                if (freespeed > 16.6667) { // > 60km/h
                    S4L1 += linkLength;
                }
            }
            if (numberLanes > 1){
                if (freespeed <= 8.33333){
                    S1L2 += linkLength;
                }
                if ((freespeed > 8.33333)&(freespeed <= 13.8889)){
                    S2L2 += linkLength;
                }
                if ((freespeed > 13.8889)&(freespeed <= 16.6667)){
                    S3L2 += linkLength;
                }
                if (freespeed > 16.6667) {
                    S4L2 += linkLength;
                }
            }

        }

        double propS1L1 = S1L1/routedDistance;
        double propS2L1 = S2L1/routedDistance;
        double propS3L1 = S3L1/routedDistance;
        double propS4L1 = S4L1/routedDistance;
        double propS1L2 = S1L2/routedDistance;
        double propS2L2 = S2L2/routedDistance;
        double propS3L2 = S3L2/routedDistance;
        double propS4L2 = S4L2/routedDistance;



        return new SwissBikeVariables(bikePredictor.predict(person,trip,elements),
                propS1L1, propS2L1, propS3L1,propS4L1,propS1L2,propS2L2,propS3L2,propS4L2, routedDistance); //g/ what unit is distance?


    }
}