package org.eqasim.switzerland.ch_cmdp.transit_dwell;

import java.util.Map;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class SampledTransitDwellConfigGroup extends ReflectiveConfigGroup {
    public static final String GROUP_NAME = "eqasim:sampledTransitDwell";
    private boolean enabled = false;

    public SampledTransitDwellConfigGroup() {
        super(GROUP_NAME);
    }

    @StringGetter("enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @StringSetter("enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put("enabled", "Scale transit passenger access/egress times by 1 / eqasim.sampleSize. "
                + "Fixed door times and schedule holding are unchanged. Default: false.");
        return comments;
    }
}
