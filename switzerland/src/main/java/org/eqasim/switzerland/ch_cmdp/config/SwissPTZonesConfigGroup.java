package org.eqasim.switzerland.ch_cmdp.config;

import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class SwissPTZonesConfigGroup extends ReflectiveConfigGroup{

    public final static String GROUP_NAME = "ptZones";

    private final static String ZONE_PATH         = "ptZonesFilePath";
    private final static String SBB_DIST_PATH     = "sbbDistancesPath";
    private final static String PRICING_DESC_PATH = "pricingDescriptionPath";

    private String zone_path = null;
    private String sbb_dist_path = null;
    private String pricing_desc_path = null;

    public SwissPTZonesConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		return map;
	}

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

    @StringSetter(ZONE_PATH)
	public void setZonePath(String zonepath) {
		this.zone_path = zonepath;
	}

    @StringSetter(SBB_DIST_PATH)
	public void setSBBDistancesPath(String sbbPath) {
		this.sbb_dist_path = sbbPath;
	}

    @StringSetter(PRICING_DESC_PATH)
	public String setPricingDescriptionPath(String pricePath) {
		return this.pricing_desc_path = pricePath;
	}

    static public SwissPTZonesConfigGroup getOrCreate(Config config) {
        SwissPTZonesConfigGroup group = (SwissPTZonesConfigGroup) config.getModules().get(GROUP_NAME);

        if (group == null){
            group = new SwissPTZonesConfigGroup();
            config.addModule(group);
        }

		return group;
	}
    
}