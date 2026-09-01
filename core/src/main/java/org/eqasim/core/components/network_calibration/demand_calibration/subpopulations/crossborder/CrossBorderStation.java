package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.scoring.RouteImpact;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.List;
import java.util.Optional;

/** One detected physical border station represented by one or two counted links. */
public record CrossBorderStation(String id, List<Id<Link>> links) {
    public CrossBorderStation {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Cross-border station ID must not be blank");
        }
        if (links == null || links.isEmpty() || links.size() > 2
                || links.stream().anyMatch(java.util.Objects::isNull)
                || links.stream().distinct().count() != links.size()) {
            throw new IllegalArgumentException(
                    "Cross-border station " + id + " requires one or two distinct links");
        }
        links = List.copyOf(links);
    }

    public CrossBorderStation(String id, Id<Link> link) {
        this(id, List.of(link));
    }

    public CrossBorderStation(String id, Id<Link> inLink, Id<Link> outLink) {
        this(id, List.of(inLink, outLink));
    }

    public Id<Link> inLink() {
        return links.getFirst();
    }

    public Optional<Id<Link>> outLink() {
        return links.size() == 2 ? Optional.of(links.get(1)) : Optional.empty();
    }

    public int passages(RouteImpact impact) {
        return links.stream().mapToInt(impact::passagesOn).sum();
    }
}
