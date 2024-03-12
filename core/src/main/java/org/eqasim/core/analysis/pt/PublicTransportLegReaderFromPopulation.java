package org.eqasim.core.analysis.pt;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopArea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PublicTransportLegReaderFromPopulation {

    final private PersonAnalysisFilter personAnalysisFilter;
    final private TransitSchedule transitSchedule;

    public PublicTransportLegReaderFromPopulation(TransitSchedule transitSchedule, PersonAnalysisFilter personAnalysisFilter) {
        this.personAnalysisFilter = personAnalysisFilter;
        this.transitSchedule = transitSchedule;

    }

    public Collection<PublicTransportLegItem> readPublicTransportLegs(String populationFilePath) {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new PopulationReader(scenario).readFile(populationFilePath);
        return this.readPublicTransportLegs(scenario.getPopulation());
    }

    public Collection<PublicTransportLegItem> readPublicTransportLegs(Population population) {
        return population.getPersons().values().stream()
                .filter(person -> personAnalysisFilter.analyzePerson(person.getId()))
                .flatMap(person -> getPublicTransportLegs(person).stream())
                .collect(Collectors.toList());
    }

    public Collection<PublicTransportLegItem> getPublicTransportLegs(Person person) {
        List<PublicTransportLegItem> legItems = new ArrayList<>();
        Plan plan = person.getSelectedPlan();
        int tripIndex = -1;
        int legIndex = -1;
        for(PlanElement planElement: plan.getPlanElements()) {
            if(planElement instanceof Activity) {
                Activity activity = (Activity) planElement;
                if(!TripStructureUtils.isStageActivityType(activity.getType())) {
                    tripIndex +=1;
                }
                continue;
            }
            Leg leg = (Leg) planElement;
            legIndex=+1;
            if(!leg.getMode().equals(TransportMode.pt)) {
                continue;
            }
            if(! (leg.getRoute() instanceof DefaultTransitPassengerRoute)) {
                throw new IllegalStateException("PT leg has invalid route type");
            }
            DefaultTransitPassengerRoute transitPassengerRoute = (DefaultTransitPassengerRoute) leg.getRoute();

            Id<TransitStopArea> accessStopAreaId = this.transitSchedule.getFacilities().get(transitPassengerRoute.getAccessStopId()).getStopAreaId();
            Id<TransitStopArea> egressStopAreaId = this.transitSchedule.getFacilities().get(transitPassengerRoute.getEgressStopId()).getStopAreaId();
            String mode = this.transitSchedule.getTransitLines().get(transitPassengerRoute.getLineId()).getRoutes().get(transitPassengerRoute.getRouteId()).getTransportMode();

            PublicTransportLegItem item = new PublicTransportLegItem(person.getId(), tripIndex, legIndex, transitPassengerRoute.getAccessStopId(), transitPassengerRoute.getEgressStopId(), transitPassengerRoute.getLineId(), transitPassengerRoute.getRouteId(), accessStopAreaId, egressStopAreaId, mode);
            legItems.add(item);
        }
        return legItems;
    }

}
