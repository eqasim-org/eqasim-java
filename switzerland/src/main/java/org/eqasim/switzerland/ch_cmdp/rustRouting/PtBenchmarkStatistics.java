// Reduces routed MATSim legs to comparable metrics without including analysis in per-query latency.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import org.matsim.api.core.v01.population.*;
import org.matsim.pt.routes.TransitPassengerRoute;
import java.util.*;

final class PtBenchmarkStatistics {
    record Result(boolean pt, double seconds, double walkSeconds, double distance, int transfers,
                  double cost, List<String> identity, String error, double latencyMs, boolean budgetLimited) {}
    private PtBenchmarkStatistics() {}
    static Result describe(List<? extends PlanElement> elements,double departure,double latencyMs,String error) {
        if(error!=null || elements==null) return missing(error,latencyMs);
        double arrival=departure,walk=0,distance=0,cost=Double.NaN;int boardings=0;
        boolean budgetLimited=false;
        List<String> identity=new ArrayList<>();
        for(var element:elements) if(element instanceof Leg leg) {
            budgetLimited |= Boolean.TRUE.equals(leg.getAttributes().getAttribute("rustRoutingBudgetLimited"));
            if(!leg.getTravelTime().isDefined()) return missing("Undefined leg travel time",latencyMs);
            double duration=leg.getTravelTime().seconds();
            arrival=leg.getDepartureTime().orElse(arrival)+duration;
            if(leg.getRoute()!=null) distance+=leg.getRoute().getDistance();
            if(leg.getRoute() instanceof TransitPassengerRoute route) {
                boardings++;
                identity.add(route.getLineId()+"|"+route.getRouteId()+"|"+route.getAccessStopId()+"|"+route.getEgressStopId());
            } else walk+=duration;
            Object value=leg.getAttributes().getAttribute(RaptorUtils.TOTAL_ROUTE_COST_ATTR_NAME);
            if(value instanceof Number number && !Double.isFinite(cost)) cost=number.doubleValue();
        }
        if(boardings==0)return missing(null,latencyMs);
        if(!Double.isFinite(arrival-departure))return missing("Invalid route time",latencyMs);
        return new Result(true,arrival-departure,walk,distance,boardings-1,cost,List.copyOf(identity),null,latencyMs,budgetLimited);
    }
    private static Result missing(String error,double latency) {
        return new Result(false,Double.NaN,Double.NaN,Double.NaN,-1,Double.NaN,List.of(),error,latency,false);
    }
    static double mean(double[] values) { return Arrays.stream(values).filter(Double::isFinite).average().orElse(Double.NaN); }
    static double median(double[] values) {
        double[] sorted=Arrays.stream(values).filter(Double::isFinite).sorted().toArray();
        if(sorted.length==0)return Double.NaN;
        int mid=sorted.length/2;return sorted.length%2==0?(sorted[mid-1]+sorted[mid])/2:sorted[mid];
    }
    static double percentile(double[] values,double fraction) {
        double[] sorted=Arrays.stream(values).filter(Double::isFinite).sorted().toArray();
        return sorted.length==0?Double.NaN:sorted[(int)Math.ceil(fraction*(sorted.length-1))];
    }
    @SuppressWarnings("null")
    static Map<String,Object> summarize(List<Result> rows,List<Double> times) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("batch_seconds",times);result.put("median_batch_seconds",median(times.stream().mapToDouble(Double::doubleValue).toArray()));
        result.put("pt_routes",rows.stream().filter(Result::pt).count());result.put("errors",rows.stream().filter(r->r.error()!=null).count());
        result.put("budget_limited_routes",rows.stream().filter(Result::budgetLimited).count());
        result.put("mean_route_seconds",mean(rows.stream().mapToDouble(Result::seconds).toArray()));
        result.put("mean_walking_seconds",mean(rows.stream().mapToDouble(Result::walkSeconds).toArray()));
        result.put("mean_distance_m",mean(rows.stream().mapToDouble(Result::distance).toArray()));
        result.put("mean_transfers",mean(rows.stream().filter(Result::pt).mapToDouble(Result::transfers).toArray()));
        result.put("mean_cost",mean(rows.stream().mapToDouble(Result::cost).toArray()));
        result.put("mean_query_ms",mean(rows.stream().mapToDouble(Result::latencyMs).toArray()));
        result.put("p50_query_ms",percentile(rows.stream().mapToDouble(Result::latencyMs).toArray(),.5));
        result.put("p99_query_ms",percentile(rows.stream().mapToDouble(Result::latencyMs).toArray(),.99));
        result.put("max_query_ms",percentile(rows.stream().mapToDouble(Result::latencyMs).toArray(),1));
        result.put("summed_query_worker_seconds",Arrays.stream(rows.stream().mapToDouble(Result::latencyMs).toArray()).sum()/1000);
        result.put("p95_query_ms",percentile(rows.stream().mapToDouble(Result::latencyMs).toArray(),.95));
        return result;
    }
    @SuppressWarnings("null")
    static Map<String,Object> compare(List<Result> reference,List<Result> candidate,double absolute,double percent) {
        int both=0,neither=0,onlyReference=0,onlyCandidate=0,similar=0,sameTransfers=0,sameSequence=0,errors=0;
        List<Double> deltas=new ArrayList<>(),distances=new ArrayList<>(),costs=new ArrayList<>();
        for(int i=0;i<reference.size();i++) {
            var a=reference.get(i);var b=candidate.get(i);
            if(a.error()!=null || b.error()!=null){errors++;continue;}
            if(a.pt() && b.pt()) {
                both++;double delta=b.seconds()-a.seconds();deltas.add(delta);
                if(Math.abs(delta)<=Math.max(absolute,Math.max(a.seconds(),b.seconds())*percent/100))similar++;
                if(a.transfers()==b.transfers())sameTransfers++;
                if(a.identity().equals(b.identity()))sameSequence++;
                distances.add(Math.abs(b.distance()-a.distance()));costs.add(Math.abs(b.cost()-a.cost()));
            } else if(a.pt())onlyReference++;else if(b.pt())onlyCandidate++;else neither++;
        }
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("pt_in_both",both);r.put("no_pt_in_both",neither);r.put("pt_only_swissrailraptor",onlyReference);r.put("pt_only_rust",onlyCandidate);r.put("requests_with_errors",errors);
        r.put("similar_travel_times",similar);r.put("same_transfer_count",sameTransfers);r.put("same_pt_line_route_stop_sequence",sameSequence);
        double[] delta=deltas.stream().mapToDouble(Double::doubleValue).toArray();
        r.put("mean_signed_time_difference_seconds",mean(delta));r.put("mean_absolute_time_difference_seconds",mean(Arrays.stream(delta).map(Math::abs).toArray()));
        r.put("p95_absolute_time_difference_seconds",percentile(Arrays.stream(delta).map(Math::abs).toArray(),.95));
        r.put("mean_absolute_distance_difference_m",mean(distances.stream().mapToDouble(Double::doubleValue).toArray()));
        r.put("mean_absolute_cost_difference",mean(costs.stream().mapToDouble(Double::doubleValue).toArray()));
        r.put("time_tolerance_seconds",absolute);r.put("time_tolerance_percent",percent);return r;
    }
}
