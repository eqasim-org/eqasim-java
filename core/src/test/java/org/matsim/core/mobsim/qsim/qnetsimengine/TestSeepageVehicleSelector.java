package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.*;
import java.lang.reflect.Proxy;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.PassingVehicleQ;
import org.matsim.vehicles.VehicleUtils;

/** Compare vehicle identity and queue state with the pinned upstream algorithm. */
public class TestSeepageVehicleSelector {
    private static final Set<String> SEEP_MODES = Set.of("bike");
    private static final ThreadLocal<?> actualScratch;
    static {
        try {
            var field = org.matsim.core.mobsim.qsim.qnetsimengine.SeepageVehicleSelector.class.getDeclaredField("SCRATCH");
            field.setAccessible(true);
            actualScratch = (ThreadLocal<?>) field.get(null);
        } catch (ReflectiveOperationException ex) { throw new ExceptionInInitializerError(ex); }
    }
    private static void checkScratch() {
        if (!((Queue<?>)actualScratch.get()).isEmpty()) throw new AssertionError("Retained vehicle references");
    }
    private static QVehicle original(PassingVehicleQ queue, double now) {
        QVehicle result = queue.peek();
        var copy = new PassingVehicleQ();
        copy.addAll(queue);
        var iterator = copy.iterator();
        while (iterator.hasNext()) {
            QVehicle vehicle = copy.poll();
            if (vehicle.getEarliestLinkExitTime() <= now && SEEP_MODES.contains(vehicle.getDriver().getMode())) {
                result = vehicle; break;
            }
        }
        return result;
    }
    private static QVehicle candidate(PassingVehicleQ queue, double now) {
        return org.matsim.core.mobsim.qsim.qnetsimengine.SeepageVehicleSelector.select(queue, now, SEEP_MODES);
    }
    @Test public void preservesSelectionAndTieOrderWithIndependentWorkerBuffers() {
        var random = new Random(55291);
        var type = VehicleUtils.createDefaultVehicleType();
        var drivers = new MobsimDriverAgent[2];
        for (int i=0;i<2;i++) {
            String mode=i==0?"car":"bike";
            drivers[i]=(MobsimDriverAgent)Proxy.newProxyInstance(TestSeepageVehicleSelector.class.getClassLoader(),new Class[]{MobsimDriverAgent.class},
                    (proxy,method,arguments)-> {if(method.getName().equals("getMode")) return mode;throw new UnsupportedOperationException(method.getName());});
        }
        var counterexample = new PassingVehicleQ();
        var bus = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId("waiting-bus"), type));
        bus.setDriver(drivers[0]); bus.setEarliestLinkExitTime(0); counterexample.add(bus);
        var bike = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId("following-bike"), type));
        bike.setDriver(drivers[1]); bike.setEarliestLinkExitTime(1); counterexample.add(bike);
        bus.setEarliestLinkExitTime(100); // Transit-stop rehandle changes a queued priority.
        if (counterexample.peek() != bus || original(counterexample, 2) != bike
                || candidate(counterexample, 2) != bike) throw new AssertionError("Transit rehandle case");
        System.out.println("unsafe_head_early_return_counterexample=true preserved_transit_rehandle=true");
        List<PassingVehicleQ> queues=new ArrayList<>();
        int checks=0;
        for(int q=0;q<2000;q++) {
            var queue=new PassingVehicleQ();
            int n=random.nextInt(64);
            for(int i=0;i<n;i++) {
                var vehicle=new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId("queue-probe-"+q+"-"+i),type));
                vehicle.setDriver(drivers[random.nextInt(4)==0?1:0]);
                vehicle.setEarliestLinkExitTime(new double[]{-0.0,0.0,-1,1,2,4,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,Double.NaN}[random.nextInt(9)]); // Ties and nonfinite priorities.
                queue.add(vehicle);
            }
            if (q % 2 == 0 && !queue.isEmpty()) {
                // Reproduce changed priorities without removing/reinserting the
                // vehicle, as the QSim transit-stop rehandle path can do.
                queue.peek().setEarliestLinkExitTime(100);
                for (var vehicle : queue) if (random.nextInt(8) == 0) vehicle.setEarliestLinkExitTime(-1);
            }
            queues.add(queue);
            for(double now:new double[]{Double.NEGATIVE_INFINITY,-1,-0.0,0,2,4,8,Double.POSITIVE_INFINITY,Double.NaN}) {
                Object[] before=queue.toArray();
                if(original(queue,now)!=candidate(queue,now)) throw new AssertionError("Different selected vehicle");
                if(!Arrays.equals(before,queue.toArray())) throw new AssertionError("Changed source heap");
                checkScratch();
                checks++;
            }
        }
        System.out.println("identity_checks="+checks+" source_heap_unchanged=true scratch_empty=true");
        var identities=java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object,Boolean>()));
        var executor=java.util.concurrent.Executors.newFixedThreadPool(6);
        try {
            var jobs=new ArrayList<java.util.concurrent.Callable<Boolean>>();
            for(int worker=0;worker<6;worker++) jobs.add(() -> {
                identities.add(actualScratch.get());
                for(int i=0;i<20000;i++) {
                    var queue=queues.get(i%queues.size());
                    if(original(queue,2)!=candidate(queue,2)) throw new AssertionError("Concurrent mismatch");
                    checkScratch();
                }
                return true;
            });
            for(var result:executor.invokeAll(jobs)) result.get();
            if(identities.size()!=6) throw new AssertionError("Scratch shared between workers");
        } catch(Exception ex) { throw new RuntimeException(ex); }
        finally { executor.shutdown(); }
        System.out.println("concurrent_checks=120000 independent_worker_buffers=6 scratch_empty=true");
    }
}
