package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;

public class SwissPtLegVariables implements BaseVariables {

    public Map<String, List<Zone>> zones;
    public double    departureTime;
    public double    arrivalTime;
    public double    sbbDistance;
    public double    networkDistance;
    public String    fromNode;
    public String    toNode;
    public String    fromNodeName;
    public String    toNodeName;

    /**
     * The stops of the leg in driving direction (from boarding to alighting), the zones of each one (all its fare
     * networks) and the distance travelled from the boarding stop to it in metres: what the regional subscriptions
     * are compared with (SwissPtRoutePredictor.uncoveredPart). Empty when unknown: the leg is then never covered.
     */
    public List<String>               stopIds          = List.of();
    public List<Collection<Zone>>     stopZones        = List.of();
    public double[]                   stopDistances_m  = new double[0];

    /** The transit mode of the route (rail, bus, tram...), empty if unknown. */
    public String    mode           = "";

    /**
     * Whether a stop of the leg is in Switzerland, and whether one is outside (SWISS_STOP): the half-fare card only
     * holds on the tickets with the Swiss fares (SwissPtStageCostCalculator.hasSwissFares). Without the stops, the leg
     * is taken to be in Switzerland.
     */
    public boolean   inSwitzerland  = true;
    public boolean   abroad         = false;

    /**
     * The stops in Switzerland, by their id (SWISS_STOP in eqasim-switzerland, utils/gtfs_router/pricing.py): the Swiss
     * stops (8501008, 8501008:0:3, Parent8501008), the SLOIDs, and the Swiss stations in the French feed
     * (StopPoint:OCETrain:::TER-85162735). Not the French coaches stopping in Geneva (FR:...).
     */
    public static final Pattern SWISS_STOP = Pattern.compile("^(?:Parent)?85\\d{5}(?::|$)|^ch:1:sloid:|^StopPoint:OCE\\w*:::\\w*-85\\d{6}$");

    public static boolean isSwissStop(String stopId) {
        return SWISS_STOP.matcher(stopId).find();
    }

    public SwissPtLegVariables(Map<String, List<Zone>> visitedZones, double departureTime, double arrivalTime, double networkDistance, double sbbDistance, String fromNode, String toNode, String fromNodeName, String toNodeName,
            List<String> stopIds, List<Collection<Zone>> stopZones, double[] stopDistances_m) {
        this(visitedZones, departureTime, arrivalTime, networkDistance, sbbDistance, fromNode, toNode, fromNodeName, toNodeName);
        if (stopIds.size() != stopZones.size() || stopIds.size() != stopDistances_m.length) {
            throw new IllegalArgumentException("One zone list and one distance are needed for each stop of the leg");
        }
        this.stopIds         = List.copyOf(stopIds);
        this.stopZones       = List.copyOf(stopZones);
        this.stopDistances_m = stopDistances_m.clone();

        if (!stopIds.isEmpty()) {
            this.inSwitzerland = stopIds.stream().anyMatch(SwissPtLegVariables::isSwissStop);
            this.abroad = !stopIds.stream().allMatch(SwissPtLegVariables::isSwissStop);
        }
    }

    public SwissPtLegVariables(Map<String, List<Zone>> visitedZones, double departureTime, double arrivalTime, double networkDistance, double sbbDistance, String fromNode, String toNode, String fromNodeName, String toNodeName) {
		this.departureTime = departureTime;
		this.arrivalTime   = arrivalTime;
        this.sbbDistance   = sbbDistance;
        this.fromNode      = fromNode;
        this.toNode        = toNode;
        this.fromNodeName  = fromNodeName;
        this.toNodeName    = toNodeName;
        this.networkDistance = networkDistance;

        this.zones = visitedZones;
	}

    public double getDepartureTime() {
        return departureTime;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getSbbDistance() {
        return sbbDistance;
    }

    public double getNetworkDistance() {
        return networkDistance;
    }

    public Map<String, List<Zone>> getZones() {
        return this.zones;
    }

    @Override
    public String toString() {
        return "Leg info: " +
                "zones=" + this.zones +
                ", from=" + this.fromNode +
                ", to=" + this.toNode +
                ", departureTime=" + this.departureTime +
                ", arrivalTime=" + this.arrivalTime +
                ", distance=" + this.networkDistance +
                ", sbbDistance=" + this.sbbDistance +
                ".";
    }
        
}
