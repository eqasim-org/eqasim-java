package org.eqasim.switzerland.ch_cmdp.utils.pricing.inputs;

import java.util.*;

public class NetworkOfDistances {
    private final Map<String, Map<String, Double>> adjacencyMap = new HashMap<>();

    public void addEdge(String from, String to, double weight) {
        adjacencyMap
            .computeIfAbsent(from, k -> new HashMap<>())
            .put(to, weight);

        adjacencyMap
            .computeIfAbsent(to, k -> new HashMap<>())
            .put(from, weight);
    }

    public double getDistance(String fromId, String toId) {
        return adjacencyMap
                .getOrDefault(fromId, Collections.emptyMap())
                .getOrDefault(toId, -1.0);
    }

    public List<String> getAllStops() {
        return new ArrayList<>(adjacencyMap.keySet());
    }
    
}
