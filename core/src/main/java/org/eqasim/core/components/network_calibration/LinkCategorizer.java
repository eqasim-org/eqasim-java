package org.eqasim.core.components.network_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles categorization of links based on OSM highway types and urban/rural distinction.
 */
public class LinkCategorizer {
    private static final Logger logger = LogManager.getLogger(LinkCategorizer.class);

    // Category 1 – Motorways / Freeways / Trunk / Expressways (highest capacity)
    public static final List<String> CATEGORY_1_HIGHWAY_TYPES = List.of(
            "motorway",
            "motorway_link",
            "trunk",
            "trunk_link"
    );

    // Category 2 – primary
    public static final List<String> CATEGORY_2_HIGHWAY_TYPES = List.of(
            "primary",
            "primary_link"
    );

    // Category 3 – Secondary
    public static final List<String> CATEGORY_3_HIGHWAY_TYPES = List.of(
            "secondary",
            "secondary_link"
    );

    // Category 4 – Tertiary
    public static final List<String> CATEGORY_4_HIGHWAY_TYPES = List.of(
            "tertiary",
            "tertiary_link"
    );

    // Category 5 – Local / Access Roads (lowest capacity)
    public static final List<String> CATEGORY_5_HIGHWAY_TYPES = List.of(
            "residential",
            "unclassified",
            "living_street",
            "service",
            "track"
    );

    public static final int UNKNOWN_CATEGORY = 0;

    private final boolean separateUrban;

    /**
     * Constructs a LinkCategorizer with the option to separate urban roads.
     * @param separateUrban Whether to treat urban roads as separate categories.
     */
    public LinkCategorizer(boolean separateUrban) {
        this.separateUrban = separateUrban;
        logger.info("LinkCategorizer initialized with separateUrban: {}", separateUrban);
    }

    /**
     * Returns all possible categories based on the separateUrban flag.
     */
    private final Set<Integer> allCategoriesUrban =  new HashSet<>(Set.of(11, 12, 13, 14, 15));
    private final Set<Integer> allCategories = Set.of(1, 2, 3, 4, 5);
    private final Set<Integer> combined = new HashSet<>();
    public List<Integer> getAllCategories() {
        if (separateUrban) {
            List<Integer> combined = new ArrayList<>(allCategories);
            combined.addAll(allCategoriesUrban);
            return combined;
        } else {
            return new ArrayList<>(allCategories);
        }
    }

    public void mergeCategories(int cat1, int cat2) {
        logger.info("Merging categories cat1={} cat2={}", cat1, cat2);
        combined.add(cat1);
        combined.add(cat2);
        int catToRemove = Math.max(cat1, cat2);
        allCategoriesUrban.remove(catToRemove);
    }

    /**
     * Determines the category of a link based on its OSM highway type and urban status.
     */
    public int getCategory(Link link) {
        if (link.getAllowedModes().contains("car")) {
            Object osmHighway = link.getAttributes().getAttribute("osm:way:highway");
            if (osmHighway != null) {
                return getCategoryFromOsmHighway(osmHighway.toString(), link);
            }
        }
        return UNKNOWN_CATEGORY;
    }

    public int getBaseCategory(Link link) {
        if (link.getAllowedModes().contains("car")) {
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

    private int getCategoryFromOsmHighway(String osmHighway, Link link) {
        int baseCategory = getBaseCategoryFromOsmHighway(osmHighway, link);

        if (separateUrban &
            baseCategory!=UNKNOWN_CATEGORY &
            !combined.contains(baseCategory) &
            isUrbanLink(link)) {
            return baseCategory + 10; // Urban categories are 11-15
        }

        return baseCategory;
    }

    /**
     * Checks if a link is in an urban area.
     */
    public boolean isUrbanLink(Link link) {
        Object municipalityTypeObj = link.getAttributes().getAttribute("municipalityType");
        if (municipalityTypeObj instanceof String municipalityType) {
            return municipalityType.equalsIgnoreCase("urban") || municipalityType.equalsIgnoreCase("urbancore");
        }
        return false;
    }

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

    /**
     * Checks if a link is outside the country.
     */
    public boolean isOutsideLink(Link link) {
        return getMunicipalityType(link).equals("outside");
    }

    public boolean isSeparateUrban() {
        return separateUrban;
    }

    public boolean isCarLink(Link link) {
        return link.getAllowedModes().contains("car");
    }
}
