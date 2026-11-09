// Compares scheduled PT routers on identical requests with shared data and private per-worker search state.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eqasim.core.components.raptor.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.*;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.io.PrintWriter;

public final class RunComparePublicTransportRouters {
    private RunComparePublicTransportRouters() {}
    private record Worker(SwissRailRaptor swiss, RustPtRoutingModule rust) {}
    private record Raw(List<? extends PlanElement> legs,double milliseconds,String error) {}
    private record Batch(List<PtBenchmarkStatistics.Result> results,double seconds) {}

    public static void main(String[] args) throws Exception {
        var cmd=new CommandLine.Builder(args).requireOptions("config-path","threads")
                .allowOptions("requests","seed","trips-path","output-path","repeats","warmup",
                        "time-tolerance-seconds","time-tolerance-percent")
                .allowPrefixes("config").build();
        int threads=Integer.parseInt(cmd.getOptionStrict("threads"));
        int count=Integer.parseInt(cmd.getOption("requests").orElse("1000"));
        int repeats=Integer.parseInt(cmd.getOption("repeats").orElse("3"));
        int warmup=Integer.parseInt(cmd.getOption("warmup").orElse(Integer.toString(count)));
        long seed=Long.parseLong(cmd.getOption("seed").orElse("6012"));
        double tolerance=Double.parseDouble(cmd.getOption("time-tolerance-seconds").orElse("60"));
        double percent=Double.parseDouble(cmd.getOption("time-tolerance-percent").orElse("5"));
        if(threads<1 || count<1 || repeats<1 || warmup<0 || !Double.isFinite(tolerance)
                || tolerance<0 || !Double.isFinite(percent) || percent<0)
            throw new IllegalArgumentException("Positive threads/requests/repeats and nonnegative warmup/tolerances required");
        Path output=Path.of(cmd.getOption("output-path").orElse("pt_router_benchmark")).toAbsolutePath();
        Files.createDirectories(output);
        var config=ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),new RustRoutingConfigGroup(),
                new EqasimRaptorConfigGroup(),new SwissRailRaptorConfigGroup());
        cmd.applyConfiguration(config);config.global().setNumberOfThreads(threads);
        var settings=ConfigUtils.addOrGetModule(config,RustRoutingConfigGroup.class);settings.enabled=true;
        RustRoutingModule.validate(config,settings);
        long start=System.nanoTime();
        var scenario=ScenarioUtils.createScenario(config);
        new org.matsim.core.network.io.MatsimNetworkReader(scenario.getNetwork()).readFile(
                ConfigGroup.getInputFileURL(config.getContext(),config.network().getInputFile()).toString());
        new org.matsim.pt.transitSchedule.api.TransitScheduleReader(scenario).readFile(
                ConfigGroup.getInputFileURL(config.getContext(),config.transit().getTransitScheduleFile()).toString());
        if(config.transit().getVehiclesFile()!=null)
            new org.matsim.vehicles.MatsimVehicleReader(scenario.getTransitVehicles()).readURL(
                    ConfigGroup.getInputFileURL(config.getContext(),config.transit().getVehiclesFile()));
        double loading=elapsed(start);
        System.out.println("Preparing identical requests; population sampling uses one streaming pass if --trips-path is absent.");
        start=System.nanoTime();
        var trips=cmd.hasOption("trips-path") ? PtBenchmarkRequests.read(Path.of(cmd.getOptionStrict("trips-path")),count)
                : PtBenchmarkRequests.sample(scenario,count,seed);
        PtBenchmarkRequests.write(output.resolve("requests.json"),trips);
        double sampling=elapsed(start);
        List<RoutingRequest> requests=requests(scenario,trips);
        start=System.nanoTime();
        var data=SwissRailRaptorData.create(scenario.getTransitSchedule(),scenario.getTransitVehicles(),
                RaptorUtils.createStaticConfig(config),scenario.getNetwork(),new OccupancyData());
        double swissPreparation=elapsed(start);
        try(var runtime=new RustRoutingRuntime(scenario,settings);var pool=Executors.newFixedThreadPool(threads)) {
            start=System.nanoTime();var ready=runtime.prepared();double rustPreparation=elapsed(start);
            System.out.println("Benchmark workers: "+threads+"; available CPUs: "+Runtime.getRuntime().availableProcessors()
                    +"; Rust native coordinate routing: "+NativePtRouter.hasCoordinates);
            var worker=ThreadLocal.withInitial(()->new Worker(swiss(data,scenario),new RustPtRoutingModule(runtime,null)));
            var barrier=new CyclicBarrier(threads);
            List<Callable<Void>> initializers=new ArrayList<>();
            for(int i=0;i<threads;i++) {
                var request=requests.get(i%requests.size());
                initializers.add(()->{
                    var w=worker.get();
                    w.swiss().calcRoute(request);w.rust().transitLegs(request);
                    barrier.await(5,TimeUnit.MINUTES);return null;
                });
            }
            start=System.nanoTime();
            for(var f:pool.invokeAll(initializers)) f.get();
            double workersPreparation=elapsed(start);
            if(warmup>0) {
                List<RoutingRequest> warm=new ArrayList<>();
                for(int i=0;i<warmup;i++) warm.add(requests.get(i%requests.size()));
                run(pool,warm,q->worker.get().swiss().calcRoute(q));
                run(pool,warm,q->worker.get().rust().transitLegs(q));
            }
            List<List<Double>> times=List.of(new ArrayList<>(),new ArrayList<>());
            List<List<PtBenchmarkStatistics.Result>> results=new ArrayList<>(Arrays.asList(null,null));
            for(int repeat=0;repeat<repeats;repeat++) {
                for(int engine:repeat%2==0?new int[]{0,1}:new int[]{1,0}) {
                    var batch=run(pool,requests,engine==0?q->worker.get().swiss().calcRoute(q):q->worker.get().rust().transitLegs(q));
                    times.get(engine).add(batch.seconds());results.set(engine,batch.results());
                    System.out.printf(Locale.ROOT,"PASS %d %-16s %.3f seconds, %.1f requests/s%n",repeat+1,
                            engine==0?"SwissRailRaptor":"Rust",batch.seconds(),count/batch.seconds());
                }
            }
            var swiss=PtBenchmarkStatistics.summarize(results.get(0),times.get(0));
            var rust=PtBenchmarkStatistics.summarize(results.get(1),times.get(1));
            var comparison=PtBenchmarkStatistics.compare(results.get(0),results.get(1),tolerance,percent);
            Map<String,Object> report=new LinkedHashMap<>();
            report.put("request_file_sha256",RustRouterInstallation.sha256(output.resolve("requests.json")));
            report.put("requests",count);report.put("threads",threads);report.put("seed",seed);report.put("repeats",repeats);
            report.put("available_processors",Runtime.getRuntime().availableProcessors());report.put("java_max_heap_bytes",Runtime.getRuntime().maxMemory());
            report.put("loading_seconds",loading);report.put("request_preparation_seconds",sampling);
            report.put("swiss_shared_preprocessing_seconds",swissPreparation);report.put("rust_shared_preprocessing_seconds",rustPreparation);
            report.put("worker_initialization_seconds",workersPreparation);report.put("warmup_requests_per_router",warmup);
            report.put("rust_max_boardings",settings.maxBoardings);report.put("rust_max_duration_seconds",settings.maxDurationSeconds);
            report.put("effective_beeline_walk_speed_m_s",ready.snapshot().parameters.getBeelineWalkSpeed());
            report.put("rust_native_coordinates",NativePtRouter.hasCoordinates);
            report.put("rust_coordinate_policy_supported",NativePtRouter.hasCoordinatePolicy);
            report.put("rust_coordinate_work_budget",settings.coordinateWorkBudget);
            report.put("rust_quality_wait_seconds",settings.qualityWaitSeconds);
            report.put("rust_quality_journey_seconds",settings.qualityJourneySeconds);
            report.put("rust_quality_radius_meters",settings.qualityRadiusMeters);
            report.put("swissrailraptor",swiss);report.put("rust",rust);report.put("comparison",comparison);
            report.put("scope","Static PT routing including access/egress and MATSim leg construction; loading/preprocessing/warmup excluded. Route and latency metrics use the last measured pass. Both indexes share the loaded scenario; this does not isolate memory per engine.");
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("summary.json").toFile(),report);
            csv(output.resolve("routes.csv"),trips,results.get(0),results.get(1));
            print(count,swiss,rust,comparison);
            System.out.println("Saved requests.json, routes.csv and summary.json to "+output);
        }
    }
    /** One instance per worker; same Eqasim utilities and mode-specific transfer calculator as simulation. */
    static SwissRailRaptor swiss(SwissRailRaptorData data,Scenario scenario) {
        var config=scenario.getConfig();
        var parameters=EqasimRaptorUtils.createParameters(config,ConfigUtils.addOrGetModule(config,EqasimRaptorConfigGroup.class),scenario.getTransitSchedule(),false);
        var builder=new SwissRailRaptor.Builder(data,config).with((RaptorParametersForPerson)person->parameters);
        if(!ConfigUtils.addOrGetModule(config,SwissRailRaptorConfigGroup.class).getModeToModeTransferPenaltyParameterSets().isEmpty())
            builder.with(new ModeSpecificTransferCostCalculator());
        return builder.build();
    }
    private static List<RoutingRequest> requests(Scenario scenario,List<PtBenchmarkRequests.Trip> trips) {
        List<RoutingRequest> result=new ArrayList<>();var factory=FacilitiesUtils.createActivityFacilities().getFactory();
        var link=scenario.getNetwork().getLinks().keySet().iterator().next();
        for(var trip:trips) {
            var from=factory.createActivityFacility(Id.create("benchmark_o_"+result.size(),ActivityFacility.class),new Coord(trip.origin()[0],trip.origin()[1]),link);
            var to=factory.createActivityFacility(Id.create("benchmark_d_"+result.size(),ActivityFacility.class),new Coord(trip.destination()[0],trip.destination()[1]),link);
            result.add(DefaultRoutingRequest.withoutAttributes(from,to,trip.departure(),null));
        }
        return result;
    }
    /** Only actual router calls contribute to query latency; route analysis runs after the batch timer. */
    private static Batch run(ExecutorService pool,List<RoutingRequest> requests,Function<RoutingRequest,List<? extends PlanElement>> router) throws Exception {
        List<Callable<Raw>> tasks=new ArrayList<>();
        for(var request:requests)tasks.add(()->{
            long start=System.nanoTime();
            try { var legs=router.apply(request);return new Raw(legs,elapsed(start)*1000,null); }
            catch(RuntimeException e) { return new Raw(null,elapsed(start)*1000,e.getClass().getSimpleName()+": "+e.getMessage()); }
        });
        long start=System.nanoTime();var futures=pool.invokeAll(tasks);
        List<Raw> raw=new ArrayList<>();for(var future:futures)raw.add(future.get());
        double seconds=elapsed(start);
        List<PtBenchmarkStatistics.Result> results=new ArrayList<>();
        for(int i=0;i<raw.size();i++) { var r=raw.get(i);results.add(PtBenchmarkStatistics.describe(r.legs(),requests.get(i).getDepartureTime(),r.milliseconds(),r.error())); }
        return new Batch(results,seconds);
    }
    private static double elapsed(long start){return (System.nanoTime()-start)/1e9;}
    private static void print(int count,Map<String,Object> swiss,Map<String,Object> rust,Map<String,Object> comparison) {
        System.out.println("\n================ PT ROUTING SUMMARY ================");
        System.out.printf("%-32s %16s %16s%n","Metric","SwissRailRaptor","Rust");
        for(String key:List.of("median_batch_seconds","pt_routes","errors","mean_route_seconds","mean_walking_seconds","mean_distance_m","mean_transfers","mean_cost","mean_query_ms","p95_query_ms","p99_query_ms","max_query_ms"))
            System.out.printf(Locale.ROOT,"%-32s %16s %16s%n",key,format(swiss.get(key)),format(rust.get(key)));
        double a=((Number)swiss.get("median_batch_seconds")).doubleValue(),b=((Number)rust.get("median_batch_seconds")).doubleValue();
        System.out.printf(Locale.ROOT,"Median throughput: SwissRailRaptor %.1f, Rust %.1f requests/s; Rust speed ratio %.3fx%n",count/a,count/b,a/b);
        comparison.forEach((key,value)->System.out.printf("%-46s %s%n",key,format(value)));
        System.out.println("No PT means no scheduled journey or walking preferred. Errors are reported separately; unavailable route values are NaN.");
    }
    private static String format(Object v){return v instanceof Double d?String.format(Locale.ROOT,"%.3f",d):String.valueOf(v);}
    private static String quote(String value){return value==null?"":"\""+value.replace("\"","\"\"")+"\"";}
    private static void csv(Path file,List<PtBenchmarkRequests.Trip> trips,List<PtBenchmarkStatistics.Result> swiss,List<PtBenchmarkStatistics.Result> rust) throws Exception {
        try(var out=new PrintWriter(Files.newBufferedWriter(file))) {
            out.println("id,departure_s,origin_x,origin_y,destination_x,destination_y,srr_pt,rust_pt,srr_time_s,rust_time_s,time_delta_s,srr_transfers,rust_transfers,srr_distance_m,rust_distance_m,srr_cost,rust_cost,srr_latency_ms,rust_latency_ms,srr_error,rust_error,rust_budget_limited");
            for(int i=0;i<trips.size();i++) {
                var q=trips.get(i);var a=swiss.get(i);var b=rust.get(i);
                out.println(i+","+q.departure()+","+q.origin()[0]+","+q.origin()[1]+","+q.destination()[0]+","+q.destination()[1]+","+a.pt()+","+b.pt()+","+a.seconds()+","+b.seconds()+","+(b.seconds()-a.seconds())+","+(a.pt()?Integer.toString(a.transfers()):"NaN")+","+(b.pt()?Integer.toString(b.transfers()):"NaN")+","+a.distance()+","+b.distance()+","+a.cost()+","+b.cost()+","+a.latencyMs()+","+b.latencyMs()+","+quote(a.error())+","+quote(b.error())+","+b.budgetLimited());
            }
        }
    }
}
