package org.eqasim.ile_de_france.probing;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;

public class ProbeNetworkRoutingModule implements RoutingModule {
    private final RoutingModule delegate;
    private final String mode;
    private final String attribute;

    public ProbeNetworkRoutingModule(RoutingModule delegate, String mode) {
        this.delegate = delegate;
        this.mode = mode;
        this.attribute = "probeTravelTime:" + mode;
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest request) {
        double probeTravelTime = (Double) request.getPerson().getAttributes().getAttribute(attribute);
        List<? extends PlanElement> elements = delegate.calcRoute(request);

        for (PlanElement element : elements) {
            if (element instanceof Leg leg) {
                if (leg.getMode().equals(mode)) {
                    leg.setTravelTime(probeTravelTime);
                    leg.getRoute().setTravelTime(probeTravelTime);
                }
            }
        }

        return elements;
    }
}
