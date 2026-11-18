package org.eqasim.switzerland.ch_cmdp.mode_choice.constraints;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class TestAirportConstraint {
    private static final List<String> MODES = List.of("car", "pt", "bike", "walk", "car_passenger");

    @Test
    public void airportAtEitherEndKeepsInitialModeAndLeavesRouteUntouched() {
        for (String initialMode : MODES) {
            for (String[] activities : List.of(new String[] {"home", "airport"},
                    new String[] {"airport", "home"}, new String[] {"airport", "airport"})) {
                var trip = trip(activities[0], activities[1], initialMode);
                var route = trip.getInitialElements().getFirst();
                var constraint = new AirportConstraint.Factory().createConstraint(null, List.of(trip), MODES);
                for (String candidate : MODES) {
                    if (initialMode.equals(candidate)) {
                        assertTrue(constraint.validateBeforeEstimation(trip, candidate, List.of()));
                    } else {
                        assertFalse(constraint.validateBeforeEstimation(trip, candidate, List.of()));
                    }
                }
                assertSame(route, trip.getInitialElements().getFirst());
            }
        }
    }

    @Test
    public void tripsWithoutAirportStillAllowModeChanges() {
        var trip = trip("home", "work", "car");
        var constraint = new AirportConstraint();
        for (String mode : MODES) {
            assertTrue(constraint.validateBeforeEstimation(trip, mode, List.of()));
        }
    }

    private static DiscreteModeChoiceTrip trip(String origin, String destination, String mode) {
        var leg = PopulationUtils.createLeg(mode);
        leg.setRoute(RouteUtils.createGenericRouteImpl(null, null));
        return new DiscreteModeChoiceTrip(PopulationUtils.createActivityFromCoord(origin, new Coord(0, 0)),
                PopulationUtils.createActivityFromCoord(destination, new Coord(1000, 0)), mode,
                List.of(leg), 0, 1, 0, new AttributesImpl());
    }
}
