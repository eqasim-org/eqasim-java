package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * The inputs of the Swiss PT fares, in the ptPricing module: the zones of the stops, the SBB distances, the fares of
 * the networks and, optionally, the Léman Pass single tickets. The paths are relative to the config file. Without the
 * Léman Pass paths (or with empty values), there is no Léman Pass.
 *
 * The configs written before the module was renamed call it ptZones: getOrCreate (called by the configurators when
 * they update the config) moves such a module into ptPricing, with a warning.
 */
public class SwissPtPricingConfigGroup extends ReflectiveConfigGroup{
    private static final Logger LOG = LogManager.getLogger(SwissPtPricingConfigGroup.class);

    public final static String GROUP_NAME = "ptPricing";

    /** The former name of the module, still read (see getOrCreate). */
    public final static String LEGACY_GROUP_NAME = "ptZones";

    private final static String ZONE_PATH              = "ptZonesFilePath";
    private final static String SBB_DIST_PATH          = "sbbDistancesPath";
    private final static String PRICING_DESC_PATH      = "pricingDescriptionPath";
    private final static String LEMAN_PASS_PRODUCTS    = "lemanPassProductsPath";
    private final static String LEMAN_PASS_STATIONS    = "lemanPassStationsPath";

    private String zone_path = null;
    private String sbb_dist_path = null;
    private String pricing_desc_path = null;
    private String leman_pass_products_path = null;
    private String leman_pass_stations_path = null;

    public SwissPtPricingConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(PRICING_DESC_PATH, "Optional: the fares of the networks. Empty: the pricing description bundled with eqasim.");
		map.put(LEMAN_PASS_PRODUCTS, "Optional: the Léman Pass single tickets (lemanpass_products.csv of eqasim-switzerland). Empty: no Léman Pass.");
		map.put(LEMAN_PASS_STATIONS, "Optional: the rail stops of the Léman Pass stations (lemanpass_stations.csv), needed with " + LEMAN_PASS_PRODUCTS + ".");
		return map;
	}

    // The getters return null for a path that is not set: the setters turn "", " " and "null" into null (see isSet)

    @StringGetter(ZONE_PATH)
	public String getZonePath() {
		return this.zone_path;
	}

    @StringGetter(SBB_DIST_PATH)
	public String getSBBDistancesPath() {
		return this.sbb_dist_path;
	}

    @StringGetter(PRICING_DESC_PATH)
	public String getPricingDescriptionPath() {
		return this.pricing_desc_path;
	}

    @StringGetter(LEMAN_PASS_PRODUCTS)
	public String getLemanPassProductsPath() {
		return this.leman_pass_products_path;
	}

    @StringGetter(LEMAN_PASS_STATIONS)
	public String getLemanPassStationsPath() {
		return this.leman_pass_stations_path;
	}

    @StringSetter(ZONE_PATH)
	public void setZonePath(String zonepath) {
		this.zone_path = orNull(zonepath);
	}

    @StringSetter(SBB_DIST_PATH)
	public void setSBBDistancesPath(String sbbPath) {
		this.sbb_dist_path = orNull(sbbPath);
	}

    @StringSetter(PRICING_DESC_PATH)
	public void setPricingDescriptionPath(String pricePath) {
		this.pricing_desc_path = orNull(pricePath);
	}

    @StringSetter(LEMAN_PASS_PRODUCTS)
	public void setLemanPassProductsPath(String path) {
		this.leman_pass_products_path = orNull(path);
	}

    @StringSetter(LEMAN_PASS_STATIONS)
	public void setLemanPassStationsPath(String path) {
		this.leman_pass_stations_path = orNull(path);
	}

    /** Whether a path is given: not null, empty, or "null" (how MATSim writes a path that is not set). */
    public static boolean isSet(String path) {
        return path != null && !path.isBlank() && !path.trim().equals("null");
    }

    private static String orNull(String path) {
        return isSet(path) ? path.trim() : null;
    }

    /**
     * For the configurators, once the config is read: a module under the former name (ptZones) becomes the ptPricing
     * module (see getOrCreate). Nothing is added to a config with neither of them.
     */
    static public void migrateLegacyModule(Config config) {
        if (config.getModules().containsKey(LEGACY_GROUP_NAME)) {
            getOrCreate(config);
        }
    }

    /**
     * The ptPricing module of a config, added if missing. A module under the former name (ptZones) is moved into it,
     * with a warning; a parameter given in both must have the same value.
     */
    static public SwissPtPricingConfigGroup getOrCreate(Config config) {
        ConfigGroup existing = config.getModules().get(GROUP_NAME);
        SwissPtPricingConfigGroup group;

        if (existing instanceof SwissPtPricingConfigGroup typed) {
            group = typed;
        } else {
            // absent, or read from the file but not yet typed: addModule takes over the parameters of a generic module
            group = new SwissPtPricingConfigGroup();
            config.addModule(group);
        }

        ConfigGroup legacy = config.getModules().get(LEGACY_GROUP_NAME);
        if (legacy != null) {
            LOG.warn("The config module '{}' is deprecated: rename it to '{}'. Its parameters are used for now.",
                    LEGACY_GROUP_NAME, GROUP_NAME);
            for (Map.Entry<String, String> entry : legacy.getParams().entrySet()) {
                if (!isSet(entry.getValue())) {
                    continue;
                }
                String current = group.getParams().get(entry.getKey());
                if (!isSet(current)) {
                    group.addParam(entry.getKey(), entry.getValue());
                } else if (!current.trim().equals(entry.getValue().trim())) {
                    throw new IllegalStateException("The config modules '" + GROUP_NAME + "' and '" + LEGACY_GROUP_NAME
                            + "' give different values for " + entry.getKey() + ": " + current + " and " + entry.getValue());
                }
            }
            config.removeModule(LEGACY_GROUP_NAME);
        }

		return group;
	}

}
