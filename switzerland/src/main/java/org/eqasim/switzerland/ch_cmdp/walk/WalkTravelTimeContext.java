package org.eqasim.switzerland.ch_cmdp.walk;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

/** Read-only inputs. Distances/elevation changes are metres and time is seconds.
 * Nearest nodes may be null on an empty network. Missing elevation or coincident nodes gives
 * nodesElevationChange=0 and hasElevation=false. Network objects must never be mutated by an expression.
 */
public record WalkTravelTimeContext(Coord origin, Coord destination, Node originNode,
        Node destinationNode, double euclideanDistance, double nodesDistance,
        double nodesElevationChange, boolean hasElevation, Person person, double departureTime,
        double defaultTravelTime) { }
