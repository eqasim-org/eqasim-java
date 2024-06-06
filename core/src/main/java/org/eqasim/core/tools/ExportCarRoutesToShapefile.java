package org.eqasim.core.tools;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ExportCarRoutesToShapefile {

    public static void main(String[] args) throws CommandLine.ConfigurationException {
        CommandLine commandLine = new CommandLine.Builder(args)
                .requireOptions("plans-path", "network-path", "crs", "output-path")
                .allowOptions("person-ids")
                .build();

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(commandLine.getOptionStrict("plans-path"));
        new MatsimNetworkReader(scenario.getNetwork()).readFile(commandLine.getOptionStrict("network-path"));
        CoordinateReferenceSystem crs = MGC.getCRS(commandLine.getOptionStrict("crs"));

        Collection<SimpleFeature> features = new LinkedList<>();

        PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder() //
                .setCrs(crs).setName("link") //
                .addAttribute("link_id", String.class) //
                .addAttribute("person_id", String.class) //
                .addAttribute("trips_d", String.class) //
                .addAttribute("leg_id", String.class) //
                .addAttribute("index", Integer.class) //
                .create();

        Set<String> personIds = commandLine.hasOption("person-ids") ? Arrays.stream(commandLine.getOptionStrict("person-ids").split(",")).collect(Collectors.toSet()) : new HashSet<>();
        Predicate<Person> personFilter;
        if(personIds.size() == 0) {
            personFilter = person -> true;
        } else {
            personFilter = person -> personIds.contains(person.getId().toString());
        }

        for(Plan plan: scenario.getPopulation().getPersons().values().stream().filter(personFilter).map(Person::getSelectedPlan).toList()) {
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan.getPlanElements());
            for(int tripIndex=0; tripIndex<trips.size(); tripIndex++) {
                for(int legIndex=0; legIndex<trips.get(tripIndex).getLegsOnly().size(); legIndex++) {
                    Leg leg = trips.get(tripIndex).getLegsOnly().get(legIndex);
                    if(leg.getMode().equals(TransportMode.car) && leg.getRoute() instanceof NetworkRoute networkRoute) {
                        for(int linkIndex=0; linkIndex<networkRoute.getLinkIds().size(); linkIndex++) {
                            Link link = scenario.getNetwork().getLinks().get(networkRoute.getLinkIds().get(linkIndex));
                            Coordinate fromCoordinate = new Coordinate(link.getFromNode().getCoord().getX(),
                                    link.getFromNode().getCoord().getY());
                            Coordinate toCoordinate = new Coordinate(link.getToNode().getCoord().getX(),
                                    link.getToNode().getCoord().getY());

                            SimpleFeature feature = linkFactory.createPolyline( //
                                    new Coordinate[] { fromCoordinate, toCoordinate }, //
                                    new Object[] { //
                                            link.getId().toString(), //
                                            plan.getPerson().getId().toString(), //
                                            tripIndex, //
                                            legIndex, //
                                            linkIndex //
                                    }, null);

                            features.add(feature);
                        }
                    }
                }
            }
        }

        ShapeFileWriter.writeGeometries(features, commandLine.getOptionStrict("output-path"));
    }
}
