package org.eqasim.core.components.network_calibration.demand_calibration;

import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.List;

public class ASCsAdapter implements IterationEndsListener {

    private final Population population;
    private final PopulationGroups populationGroups;
    private final TripListConverter tripListConverter;
    private final ODErrors odErrors;
    private final double dmcWeight;
    private final boolean activate;

    public ASCsAdapter(Scenario scenario, PopulationGroups populationGroups, TripListConverter tripListConverter,
                       ODErrors odErrors,  double dmcWeight, boolean activate) {
        this.population = scenario.getPopulation();
        this.populationGroups = populationGroups;
        this.tripListConverter = tripListConverter;
        this.odErrors = odErrors;
        this.dmcWeight = dmcWeight;
        this.activate = activate;

        for (Person person: scenario.getPopulation().getPersons().values()){
            Tools.setCarASC(person, 0.0);
        }
    }

    public void updateASCs() {
        int[][] OD = odErrors.getODErrors();
        for (Person person : population.getPersons().values()) {
            List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(person.getSelectedPlan());
            for (DiscreteModeChoiceTrip trip : trips) {
                Coord origin = trip.getOriginActivity().getCoord();
                Coord destination = trip.getDestinationActivity().getCoord();
                int groupOrigin = populationGroups.getGroup(origin);
                int groupDestination = populationGroups.getGroup(destination);
                int odError = OD[groupOrigin][groupDestination];
                double deltaAsc = getDeltaAsc(odError);
                Tools.incrementCarASC(person, deltaAsc);
            }
        }
    }

    private double getDeltaAsc(int odError){
        double scale = 1.0; // TODO: implement proper formula
        double deltaAsc = odError * scale;

        return deltaAsc;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (!activate) {return;}
        // if this is activated, we do this each time all population did mode choice
        int interval = (int) Math.floor(1.0 / dmcWeight);
        int iteration = event.getIteration();
        if (iteration%interval==0) {
            updateASCs();
        }
    }
}
