package org.eqasim.switzerland.ch_cmdp.walk;

import java.util.List;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;

/** Uses MATSim for route construction and distance, replacing only the duration. */
final class WalkRoutingModule implements RoutingModule {
    private final RoutingModule delegate;
    private final WalkTravelTimeCalculator calculator;

    WalkRoutingModule(RoutingModule delegate, WalkTravelTimeCalculator calculator) {
        this.delegate = delegate;
        this.calculator = calculator;
    }

    @Override public List<? extends PlanElement> calcRoute(RoutingRequest request) {
        var result = delegate.calcRoute(request);
        if (!calculator.isDefault()) for (var element : result) {
            Leg leg = (Leg) element;
            double time = calculator.calculate(request.getFromFacility().getCoord(),
                    request.getToFacility().getCoord(), request.getPerson(), request.getDepartureTime(),
                    leg.getTravelTime().seconds());
            leg.setTravelTime(time);
            leg.getRoute().setTravelTime(time);
        }
        return result;
    }
}
