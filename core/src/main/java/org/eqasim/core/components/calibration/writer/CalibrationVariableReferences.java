package org.eqasim.core.components.calibration.writer;

import java.util.IdentityHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/** Worker-local export references, retained only for the person currently being evaluated. */
public final class CalibrationVariableReferences {
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static volatile boolean active;
    private static volatile long generation;

    private CalibrationVariableReferences() { }

    public static void start() { generation++; active = true; }
    public static void stop() { active = false; STATE.remove(); }
    public static boolean isActive() { return active; }

    /** Called before estimating a trip; cached candidates retain their previously bound reference. */
    public static void beginPerson(Person person) {
        if (!active) return;
        State state = STATE.get();
        if (state.person != person || state.generation != generation) {
            state.references.clear();
            state.person = person;
            state.generation = generation;
        }
        state.pending = -1;
    }

    public static long record() {
        long id = NEXT_ID.incrementAndGet();
        STATE.get().pending = id;
        return id;
    }

    /** Associates the returned candidate with the inputs exported by its utility estimator. */
    public static void bind(TripCandidate candidate) {
        if (!active) return;
        State state = STATE.get();
        if (candidate != null && state.pending >= 0) state.references.put(candidate, state.pending);
        state.pending = -1;
    }

    public static long get(TripCandidate candidate) {
        return active ? STATE.get().references.getOrDefault(candidate, -1L) : -1;
    }

    private static final class State {
        final IdentityHashMap<TripCandidate, Long> references = new IdentityHashMap<>();
        Person person;
        long generation = -1;
        long pending = -1;
    }
}
