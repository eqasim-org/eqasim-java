package org.eqasim.core.components.network_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyGroupKey;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreespeedCalibrationKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles categorization of links based on OSM highway types and urban/rural distinction.
 */
public class LinkCategorizer {
    private static final Logger logger = LogManager.getLogger(LinkCategorizer.class);

    public static final Set<String> CATEGORY_1_HIGHWAY_TYPES = Set.of(
                                    "motorway", "motorway_link", "trunk", "trunk_link");
    public static final Set<String> CATEGORY_2_HIGHWAY_TYPES = Set.of(
                                    "primary", "primary_link");
    public static final Set<String> CATEGORY_3_HIGHWAY_TYPES = Set.of(
                                    "secondary", "secondary_link");
    public static final Set<String> CATEGORY_4_HIGHWAY_TYPES = Set.of(
                                    "tertiary", "tertiary_link");
    public static final Set<String> CATEGORY_5_HIGHWAY_TYPES = Set.of(
                                    "residential", "unclassified", "living_street", "service", "track");
    public static final int UNKNOWN_CATEGORY = 0;

    private final Map<Id<Link>, Integer> penaltiesSpecialRegionByLinkId = new HashMap<>();
    private final Map<Id<Link>, Integer> freespeedSpecialRegionByLinkId = new HashMap<>();
    private final boolean hasPenaltySpecialRegions;
    private final boolean hasFreespeedSpecialRegions;

    /**
     * Constructs a LinkCategorizer. Urban/rural separation is always active.
     * @param config Network Calibration Config group.
     */
    public LinkCategorizer(Network network, NetworkCalibrationConfigGroup config) {
        List<String> penaltiesSpecialRegionFiles = config.getPenaltiesSpecialRegionFiles();
        this.hasPenaltySpecialRegions = !penaltiesSpecialRegionFiles.isEmpty();

        List<String> freespeedSpecialRegionFiles = config.getFreespeedSpecialRegionFiles();
        this.hasFreespeedSpecialRegions = !freespeedSpecialRegionFiles.isEmpty();

        initSpecialRegions(network, penaltiesSpecialRegionFiles, freespeedSpecialRegionFiles);
    }

    /**
     * Keys management
     */

    public PenaltyGroupKey getPenaltyGroupKey(Link link) {
        int linkCategory = getBaseCategory(link);
        if (!isCarLink(link) || linkCategory == UNKNOWN_CATEGORY || isOutsideLink(link)) {
            return null;
        }

        return new PenaltyGroupKey(linkCategory, isUrbanLink(link), getPenaltiesSpecialRegionId(link));
    }

    public FreespeedCalibrationKey getFreespeedCalibrationKey(Link link) {
        int category = getBaseCategory(link);
        if (!isCarLink(link) || category == UNKNOWN_CATEGORY) {
            return null;
        }

        return new FreespeedCalibrationKey(category, getMunicipalityType(link), getFreespeedSpecialRegionId(link));
    }

    /**
     * Special regions
     */

    public int getPenaltiesSpecialRegionId(Link link) {
        return penaltiesSpecialRegionByLinkId.getOrDefault(link.getId(), 0);
    }

    public int getFreespeedSpecialRegionId(Link link) {
        return freespeedSpecialRegionByLinkId.getOrDefault(link.getId(), 0);
    }

    private void initSpecialRegions(Network network, List<String> penaltiesSpecialRegionFiles, List<String> freespeedSpecialRegionFiles) {
        if (hasPenaltySpecialRegions) {
            for (int index = 0; index < penaltiesSpecialRegionFiles.size(); index++) {
                String file = penaltiesSpecialRegionFiles.get(index);
                int specialRegionId = index + 1;
                SpecialRegionsReader specialRegionsReader = new SpecialRegionsReader(file);
                logger.info("Loaded special region file {} (id={}) with {} geometries.",
                        file, specialRegionId, specialRegionsReader.getNumberOfRegions());

                for (Id<Link> linkId : specialRegionsReader.getLinksInSpecialRegions(network)) {
                    penaltiesSpecialRegionByLinkId.putIfAbsent(linkId, specialRegionId);
                }
            }

            logger.info("Network has {} links assigned to penalties special regions.", penaltiesSpecialRegionByLinkId.size());
        }


        if (hasFreespeedSpecialRegions) {
            for (int index = 0; index < freespeedSpecialRegionFiles.size(); index++) {
                String file = freespeedSpecialRegionFiles.get(index);
                int specialRegionId = index + 1;
                SpecialRegionsReader specialRegionsReader = new SpecialRegionsReader(file);
                logger.info("Loaded freespeed special region file {} (id={}) with {} geometries.",
                        file, specialRegionId, specialRegionsReader.getNumberOfRegions());

                for (Id<Link> linkId : specialRegionsReader.getLinksInSpecialRegions(network)) {
                    freespeedSpecialRegionByLinkId.putIfAbsent(linkId, specialRegionId);
                }
            }

            logger.info("Network has {} links assigned to freespeed special regions.", freespeedSpecialRegionByLinkId.size());
        }
    }

    /**
     * Osm Categories
     */

    public int getBaseCategory(Link link) {
        if (isCarLink(link)) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            if (osmHighway != null) {
                return getBaseCategoryFromOsmHighway(osmHighway.toString(), link);
            }
        }
        return UNKNOWN_CATEGORY;
    }

    private int getBaseCategoryFromOsmHighway(String osmHighway, Link link) {
        int baseCategory;
        if (CATEGORY_1_HIGHWAY_TYPES.contains(osmHighway)) {
            baseCategory = 1;
        } else if (CATEGORY_2_HIGHWAY_TYPES.contains(osmHighway)) {
            baseCategory = 2;
        } else if (CATEGORY_3_HIGHWAY_TYPES.contains(osmHighway)) {
            baseCategory = 3;
        } else if (CATEGORY_4_HIGHWAY_TYPES.contains(osmHighway)) {
            baseCategory = 4;
        } else if (CATEGORY_5_HIGHWAY_TYPES.contains(osmHighway)) {
            if (link.getNumberOfLanes() > 1 || link.getFreespeed() > 45 / 3.6) {
                baseCategory = 4; // treat as tertiary if more than 1 lane or freespeed > 45 km/h
            } else {
                baseCategory = 5;
            }
        } else {
            return UNKNOWN_CATEGORY; // unknown category
        }
        return baseCategory;
    }

    /**
    * helper methods
    */

    public String getMunicipalityType(Link link) {
        Object municipalityTypeObj = link.getAttributes().getAttribute("municipalityType");
        if (municipalityTypeObj instanceof String municipalityType) {
            String normalized = municipalityType.trim().toLowerCase();
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "unknown";
    }

    public boolean isOutsideLink(Link link) {
        return getMunicipalityType(link).equals("outside");
    }

    public boolean isCarLink(Link link) {
        return link.getAllowedModes().contains("car");
    }

    public boolean isUrbanLink(Link link) {
        String municipalityType = getMunicipalityType(link);
        return municipalityType.equalsIgnoreCase("urban") || municipalityType.equalsIgnoreCase("urbancore");
    }
}
