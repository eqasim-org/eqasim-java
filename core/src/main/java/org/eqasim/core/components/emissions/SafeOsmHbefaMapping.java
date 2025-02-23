package org.eqasim.core.components.emissions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.core.network.NetworkUtils;

// This class is designed to wrap the matsim-libs emissions contrib OsmHbefaMapping by providing a default hbefa type for unknown osm keys (instead of throwing a RuntimeException
public class SafeOsmHbefaMapping extends HbefaRoadTypeMapping {

    private final static OsmHbefaMapping osmHbefaMapping = OsmHbefaMapping.build();
    private final static Logger log = LogManager.getLogger(SafeOsmHbefaMapping.class);
    public static String defaultType = "URB/Access/30";

    @Override
    public String determineHbefaType(Link link) {
        String result;
        try {
            result = osmHbefaMapping.determineHbefaType(link);
        } catch (RuntimeException runtimeException) {
            String type = (String) link.getAttributes().getAttribute(NetworkUtils.TYPE);
            log.warn("'" + type + "' not in hbefa map; setting to " + defaultType);
            result = defaultType;
        }
        return result;
    }

}
