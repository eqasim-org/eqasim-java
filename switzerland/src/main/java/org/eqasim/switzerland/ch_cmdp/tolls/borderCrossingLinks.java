package org.eqasim.switzerland.ch_cmdp.tolls;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/** One immutable border-link index per simulation, shared by all car/truck routing workers. */
@Singleton
public final class borderCrossingLinks {
    public static final double DISUTILITY_SECONDS = 3 * 60.0;
    private final Set<Id<Link>> linkIds;

    @Inject public borderCrossingLinks(Network network) { this(network.getLinks().values()); }

    public borderCrossingLinks(Collection<? extends Link> links) {
        var ids = new HashSet<Id<Link>>();
        for (var link : links) {
            Object value = link.getAttributes().getAttribute("ch_border_crossing");
            if (Boolean.TRUE.equals(value) || value instanceof String text && Boolean.parseBoolean(text))
                ids.add(link.getId());
        }
        linkIds = Set.copyOf(ids);
    }

    public Set<Id<Link>> getLinkIds() { return linkIds; }

    public double getDisutility(Link link) {
        return linkIds.contains(link.getId()) ? DISUTILITY_SECONDS : 0.0;
    }
}
