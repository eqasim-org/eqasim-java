package org.eqasim.core.components.network_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyGroupKey;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreespeedCalibrationKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Categorizes links by road type and location. Freespeed groups capture the input
 * network before factor application; penalty categories follow live link properties.
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
    // Ten immutable group keys per configured region, shared by all links and
    // routing workers. Cache the key values, not a link's changing category.
    private final PenaltyGroupKey[][][] penaltyKeys;
    // Indexed by link ID so filtered routing copies use exactly the same group.
    // Values are immutable and shared by links belonging to the same group.
    private final IdMap<Link, FreespeedCalibrationKey> freespeedKeys = new IdMap<>(Link.class);
    private final double categoryFivePromotionLaneThreshold;
    private final double categoryFivePromotionSpeedThreshold;

    /**
     * Constructs a LinkCategorizer. Urban/rural separation is always active.
     * @param config Network Calibration Config group.
     */
    public LinkCategorizer(Network network, NetworkCalibrationConfigGroup config) {
        this.categoryFivePromotionLaneThreshold = config.getCategoryFivePromotionLaneThreshold();
        this.categoryFivePromotionSpeedThreshold = config.getCategoryFivePromotionSpeedThreshold() / 3.6;
        // Fixed link attributes already contain the regional calibration result.
        // Only calibration or an explicit group CSV needs spatial classification.
        var costConfig = config.getCostCalibrationConfigGroup();
        boolean penaltyGroupsNeeded = config.isActivated() && config.isLinkPenaltyActivated()
                && (config.isLinkPenaltyCalibrationActivated()
                    || (costConfig.hasPenaltiesFile() && !costConfig.getResetPenaltiesToZeros()));
        List<String> penaltiesSpecialRegionFiles = penaltyGroupsNeeded ? costConfig.getSpecialRegionFiles() : List.of();
        this.hasPenaltySpecialRegions = !penaltiesSpecialRegionFiles.isEmpty();

        var speedConfig = config.getFreeSpeedCalibrationConfigGroup();
        boolean freespeedGroupsNeeded = config.isActivated() && config.isFreeSpeedFactorActivated()
                && (config.isFreeSpeedFactorCalibrationActivated() || speedConfig.hasFactorsFile());
        List<String> freespeedSpecialRegionFiles = freespeedGroupsNeeded ? speedConfig.getSpecialRegionFiles() : List.of();
        this.hasFreespeedSpecialRegions = !freespeedSpecialRegionFiles.isEmpty();

        initSpecialRegions(network, penaltiesSpecialRegionFiles, freespeedSpecialRegionFiles);
        initializeFreespeedGroups(network);
        penaltyKeys = new PenaltyGroupKey[5][2][penaltiesSpecialRegionFiles.size() + 1];
        for (int category = 1; category <= 5; category++) {
            for (int urban = 0; urban <= 1; urban++) {
                for (int region = 0; region <= penaltiesSpecialRegionFiles.size(); region++) {
                    penaltyKeys[category - 1][urban][region] = new PenaltyGroupKey(category, urban == 1, region);
                }
            }
        }
    }

    /**
     * Keys management
     */

    public PenaltyGroupKey getPenaltyGroupKey(Link link) {
        int linkCategory = getBaseCategory(link);
        if (!isCarLink(link) || linkCategory == UNKNOWN_CATEGORY || isOutsideLink(link)) {
            return null;
        }

        // Re-evaluate current lanes/freespeed and attributes above: calibration
        // can promote category five to four, and penalties are updated separately.
        boolean urban = isUrbanLink(link);
        int region = getPenaltiesSpecialRegionId(link);
        if (linkCategory >= 1 && linkCategory <= 5 && region >= 0 && region < penaltyKeys[0][0].length) {
            return penaltyKeys[linkCategory - 1][urban ? 1 : 0][region];
        }
        // Preserve extension/subclass behavior for regions outside the configured pool.
        return new PenaltyGroupKey(linkCategory, urban, region);
    }

    /** Stable input-network group, also for routing copies with the same link ID; null if initially ineligible. */
    public FreespeedCalibrationKey getFreespeedCalibrationKey(Link link) {
        return freespeedKeys.get(link.getId());
    }

    /** Capture groups before initial factors are applied; calibration must not regroup its own observations. */
    private void initializeFreespeedGroups(Network network) {
        Map<FreespeedCalibrationKey, FreespeedCalibrationKey> groups = new HashMap<>();
        for (Link link : network.getLinks().values()) {
            int category = getBaseCategory(link);
            if (category == UNKNOWN_CATEGORY) continue;
            var key = new FreespeedCalibrationKey(category, getMunicipalityType(link), getFreespeedSpecialRegionId(link));
            freespeedKeys.put(link.getId(), groups.computeIfAbsent(key, v -> v));
        }
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
            if (link.getNumberOfLanes() > categoryFivePromotionLaneThreshold
                    || link.getFreespeed() > categoryFivePromotionSpeedThreshold) {
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
