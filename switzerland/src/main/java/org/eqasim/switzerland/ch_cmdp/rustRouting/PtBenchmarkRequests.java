// Streams a deterministic sample of real plan trips, or replays saved coordinate requests.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import java.nio.file.*;
import java.util.*;

final class PtBenchmarkRequests {
    record Trip(double[] origin, double[] destination, double departure) {
        Trip {
            if (origin == null || destination == null || origin.length != 2 || destination.length != 2 || !Double.isFinite(departure)
                    || departure < 0 || !Double.isFinite(origin[0]) || !Double.isFinite(origin[1])
                    || !Double.isFinite(destination[0]) || !Double.isFinite(destination[1]))
                throw new IllegalArgumentException("Invalid benchmark coordinates/departure");
        }
    }
    private PtBenchmarkRequests() {}
    static List<Trip> read(Path file, int count) throws Exception {
        var root = new ObjectMapper().readTree(file.toFile());
        if (!root.isArray() || root.size() < count)
            throw new IllegalArgumentException("Request file needs at least " + count + " entries; use --requests to change the count");
        List<Trip> result = new ArrayList<>();
        for (int i=0;i<count;i++) {
            var q=root.get(i);var o=q.required("origin");var d=q.required("destination");var t=q.required("departure");
            if (o.size()!=2 || d.size()!=2 || !o.get(0).isNumber() || !o.get(1).isNumber()
                    || !d.get(0).isNumber() || !d.get(1).isNumber())
                throw new IllegalArgumentException("Invalid coordinate pair at request " + i);
            result.add(new Trip(new double[]{o.get(0).asDouble(),o.get(1).asDouble()},
                    new double[]{d.get(0).asDouble(),d.get(1).asDouble()},t.isNumber()?t.asDouble():Time.parseTime(t.asText())));
        }
        return result;
    }
    static final class Reservoir {
        final int size; final Random random; long eligible; final List<Trip> trips=new ArrayList<>();
        Reservoir(int size,long seed) { this.size=size;random=new Random(seed); }
        void accept(Trip trip) {
            eligible++;
            if (trips.size()<size) trips.add(trip);
            else { long slot=random.nextLong(eligible); if(slot<size) trips.set((int)slot,trip); }
        }
    }
    /** One streaming pass retains only the sampled coordinates, never the full population. */
    static List<Trip> sample(Scenario scenario,int count,long seed) throws Exception {
        var reservoir=new Reservoir(count,seed);
        var reader=new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        reader.addAlgorithm(person->{
            String sub=PopulationUtils.getSubpopulation(person);
            Object freight=person.getAttributes().getAttribute("isFreight");
            if (Boolean.TRUE.equals(freight) || "true".equalsIgnoreCase(String.valueOf(freight)) || "1".equals(String.valueOf(freight))
                    || (sub!=null && (sub.toLowerCase(Locale.ROOT).contains("freight") || sub.equalsIgnoreCase("truck")))) return;
            var plan=person.getSelectedPlan();if(plan==null)return;
            for(var trip:TripStructureUtils.getTrips(plan)) {
                var from=trip.getOriginActivity();var to=trip.getDestinationActivity();
                Coord a=coordinate(from,scenario),b=coordinate(to,scenario);
                double departure=from.getEndTime().orElse(Double.NaN);
                if (!Double.isFinite(departure))
                    for(var element:trip.getTripElements()) if(element instanceof Leg leg && leg.getDepartureTime().isDefined()) { departure=leg.getDepartureTime().seconds();break; }
                if(a==null || b==null || !Double.isFinite(departure) || departure<5*3600 || departure>22*3600
                        || !Double.isFinite(a.getX()) || !Double.isFinite(a.getY())
                        || !Double.isFinite(b.getX()) || !Double.isFinite(b.getY())
                        || Math.hypot(a.getX()-b.getX(),a.getY()-b.getY())<200) continue;
                reservoir.accept(new Trip(new double[]{a.getX(),a.getY()},new double[]{b.getX(),b.getY()},departure));
            }
        });
        var config=scenario.getConfig();
        if(config.plans().getInputFile()==null) throw new IllegalArgumentException("Config has no plans input; supply --trips-path");
        reader.readURL(ConfigGroup.getInputFileURL(config.getContext(),config.plans().getInputFile()));
        System.out.println("Eligible population trips: "+reservoir.eligible+" (05:00-22:00, >=200 m, excluding freight)");
        if(reservoir.trips.size()<count) throw new IllegalArgumentException("Only "+reservoir.trips.size()+" eligible trips; reduce --requests or supply --trips-path");
        return reservoir.trips;
    }
    private static Coord coordinate(Activity activity,Scenario scenario) {
        if(activity.getCoord()!=null)return activity.getCoord();
        if(activity.getLinkId()==null)return null;
        var link=scenario.getNetwork().getLinks().get(activity.getLinkId());
        return link==null?null:link.getCoord();
    }
    static void write(Path file,List<Trip> trips) throws Exception {
        var rows=trips.stream().map(q->Map.of("origin",q.origin(),"destination",q.destination(),"departure",q.departure())).toList();
        new ObjectMapper().enable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writerWithDefaultPrettyPrinter().writeValue(file.toFile(),rows);
    }
}
