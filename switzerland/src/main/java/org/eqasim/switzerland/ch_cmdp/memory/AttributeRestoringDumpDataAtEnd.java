package org.eqasim.switzerland.ch_cmdp.memory;

import com.google.inject.Singleton;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ShutdownListener;

/** Decorates the original final writer, including its core-listener position. No upstream class is replaced. */
public final class AttributeRestoringDumpDataAtEnd implements DumpDataAtEnd, ShutdownListener {
    private final ShutdownListener delegate;
    private final Runnable before;
    private final Runnable after;

    /** Shared final-export boundary for runtime scenario adaptations and attribute cleanup. */
    public static AbstractModule module(Runnable before, Runnable after, ShutdownListener lifecycle) {
        Class<? extends ShutdownListener> implementation = originalWriterType();
        return new AbstractModule() {
            @Override public void install() {
                bind(implementation).in(Singleton.class);
                var original = binder().getProvider(implementation);
                bind(DumpDataAtEnd.class).toProvider(() ->
                        new AttributeRestoringDumpDataAtEnd(original.get(), before, after)).in(Singleton.class);
                addControllerListenerBinding().toInstance(lifecycle);
            }
        };
    }

    public static AbstractModule module(ScenarioAttributeArchive archive) {
        return module(archive::restore, () -> {}, archive);
    }

    private AttributeRestoringDumpDataAtEnd(ShutdownListener delegate, Runnable before, Runnable after) {
        this.delegate = delegate; this.before = before; this.after = after;
    }

    /** Package-private in MATSim: resolve its type once; Guice handles construction and member injection. */
    private static Class<? extends ShutdownListener> originalWriterType() {
        try {
            var implementation = Class.forName("org.matsim.core.controler.corelisteners.DumpDataAtEndImpl");
            if (!DumpDataAtEnd.class.isAssignableFrom(implementation))
                throw new IllegalStateException("MATSim's final writer no longer implements DumpDataAtEnd");
            return implementation.asSubclass(ShutdownListener.class);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IllegalStateException("MATSim's final writer changed; update the attribute restoration adapter", e);
        }
    }

    @Override public double priority() { return delegate.priority(); }

    @Override public void notifyShutdown(ShutdownEvent event) {
        if (!event.isUnexpected()) {
            before.run();
        }
        delegate.notifyShutdown(event);
        if (!event.isUnexpected()) after.run();
    }
}
