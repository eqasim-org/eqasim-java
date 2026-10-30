package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

/** Observes actual strategy assignments, without selecting people or changing worker assignment. */
final class PtCachePrefetchingStrategy implements PlanStrategy {
    private final PlanStrategy delegate;
    private final PtRouteCache cache;
    private final IntArrayList selected = new IntArrayList();

    PtCachePrefetchingStrategy(PlanStrategy delegate, PtRouteCache cache) {
        this.delegate = delegate; this.cache = cache;
    }

    @Override public void init(ReplanningContext context) {
        selected.clear(); delegate.init(context);
    }

    @Override public void run(HasPlansAndId<Plan, Person> person) {
        selected.add(person.getId().index()); delegate.run(person);
    }

    @Override public void finish() {
        try { cache.prefetch(selected); delegate.finish(); }
        finally { selected.clear(); }
    }

    // StrategyWeightDecay and network calibration identify the strategy by this description.
    @Override public String toString() { return delegate.toString(); }
}
