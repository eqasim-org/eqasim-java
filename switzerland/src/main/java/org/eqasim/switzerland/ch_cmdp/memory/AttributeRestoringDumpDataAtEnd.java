package org.eqasim.switzerland.ch_cmdp.memory;

import com.google.inject.Singleton;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.listener.ShutdownListener;

/** Decorates the original final writer, including its core-listener position. No upstream class is replaced. */
public final class AttributeRestoringDumpDataAtEnd implements DumpDataAtEnd, ShutdownListener {
    private final ShutdownListener delegate;
    private final ScenarioAttributeArchive archive;

    public static AbstractModule module(ScenarioAttributeArchive archive) {
        Class<? extends ShutdownListener> implementation = originalWriterType();
        return new AbstractModule() {
            @Override public void install() {
                bind(ScenarioAttributeArchive.class).toInstance(archive);
                bind(implementation).in(Singleton.class);
                var original = binder().getProvider(implementation);
                bind(DumpDataAtEnd.class).toProvider(() ->
                        new AttributeRestoringDumpDataAtEnd(original.get(), archive)).in(Singleton.class);
                addControllerListenerBinding().toInstance(archive);
            }
        };
    }

    private AttributeRestoringDumpDataAtEnd(ShutdownListener delegate, ScenarioAttributeArchive archive) {
        this.delegate = delegate; this.archive = archive;
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
        if (!event.isUnexpected()) archive.restore();
        delegate.notifyShutdown(event);
    }
}
