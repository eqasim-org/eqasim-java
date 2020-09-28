package org.eqasim.jakarta.roadpricing;



import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

public final class JakartaMcRoadPricingConfigGroup extends ReflectiveConfigGroup {
	// has to be public

	/* Reason for removing "usingRoadPricing" switch: We found it hard to 
	 * interpret. Should a script "set" this switch, or rather "interpret" 
	 * it, or ignore it? For the Gauteng toll simulation, it had to be set 
	 * "false" in order to make everything work correctly. It is now gone; 
	 * if you want to simulate a non-toll base case, recommendation is to 
	 * use an empty toll file. In that way, you can be confident that you 
	 * do not get two different execution paths which may cause differences 
	 * by themselves. kai, in consultation with michael z. and johan j, sep'14
	 */

	public static final String GROUP_NAME = "roadpricingmc";

	private static final String TOLL_LINKS_FILE = "tollLinksFile";
	private String tollLinksFile = null;

	public JakartaMcRoadPricingConfigGroup() {
		super(GROUP_NAME);
	}


    @Override
    public Map<String, String> getComments() {
        Map<String,String> map = super.getComments();
        return map;
    }

    @StringGetter(TOLL_LINKS_FILE)
    public String getTollLinksFile() {
		return this.tollLinksFile;
	}
    @StringSetter(TOLL_LINKS_FILE)
	public void setTollLinksFile(final String tollLinksFile) {
		this.tollLinksFile = tollLinksFile;
	}
}

