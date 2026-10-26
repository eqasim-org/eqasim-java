package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;

/** Each worker keeps its current router; the immutable serialized itineraries are shared. */
public final class CachingPtRoutingModule implements RoutingModule {
    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(CachingPtRoutingModule.class);
    private final RoutingModule delegate;
    private final PtRouteCache cache;
    private final PopulationFactory factory;

    public CachingPtRoutingModule(RoutingModule delegate, PtRouteCache cache, PopulationFactory factory) {
        this.delegate = delegate; this.cache = cache; this.factory = factory;
    }

    @Override public List<? extends PlanElement> calcRoute(RoutingRequest request) {
        cache.requests.increment();
        PtRouteKey key;
        try { key = PtRouteKey.of(request, cache.generation()); }
        catch (IOException | UnsupportedOperationException e) {
            bypass(e); return delegate.calcRoute(request);
        }
        byte[] value = cache.get(key);
        if (value != null) return restoreOrRoute(value, request);
        var promise = new CompletableFuture<byte[]>();
        var running = cache.inFlight.putIfAbsent(key, promise);
        if (running != null) {
            cache.coalesced.increment();
            try {
                value = running.join();
                return value == null ? delegate.calcRoute(request) : restoreOrRoute(value, request);
            } catch (CompletionException e) {
                if (e.getCause() instanceof RuntimeException runtime) throw runtime;
                if (e.getCause() instanceof Error error) throw error;
                throw e;
            }
        }
        try {
            // Another owner may have completed between the first lookup and acquiring this key.
            value = cache.get(key);
            if (value != null) { promise.complete(value); return restoreOrRoute(value, request); }
            cache.misses.increment();
            var result = delegate.calcRoute(request);
            try { value = PtItineraryCodec.encode(result); cache.put(key, value); }
            catch (IOException | UnsupportedOperationException e) { bypass(e); value = null; }
            promise.complete(value);
            return result;
        } catch (RuntimeException | Error e) {
            promise.completeExceptionally(e); throw e;
        } finally { cache.inFlight.remove(key, promise); }
    }

    private List<? extends PlanElement> restoreOrRoute(byte[] value, RoutingRequest request) {
        try { return PtItineraryCodec.decode(value, factory); }
        catch (IOException | RuntimeException e) {
            cache.bypasses.increment(); cache.invalidate();
            LOG.warn("Could not restore PT cache record; rerouting", e);
            return delegate.calcRoute(request);
        }
    }

    private void bypass(Exception reason) {
        cache.bypasses.increment();
        if (cache.bypasses.sum() == 1) LOG.info("PT route cache first unsupported input/output: {}", reason.toString());
    }
}
