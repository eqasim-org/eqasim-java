package org.eqasim.core.components.traffic_light.delays.shahpar;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashMap;
import java.util.Map;

public class ShahparConfigGroup extends ReflectiveConfigGroup {
    static public final String GROUP_NAME = "shahparParameters";

    public static final String ALPHA = "alpha";
    public static final String BETA = "beta";
    public static final String ETA = "ETA";

    private double alpha = 3.5;
    private double beta = 4.85;
    private double eta = 1.48;

    public ShahparConfigGroup() {
        super(GROUP_NAME);
    }

    @Override
    public Map<String, String> getComments(){
        Map<String, String> comments = new HashMap<>();
        comments.put(ALPHA, "Alpha parameter for the Shahpar delay model (default: 3.5)");
        comments.put(BETA, "Beta parameter for the Shahpar delay model (default: 4.85)");
        comments.put(ETA, "Eta parameter for the Shahpar delay model (default: 1.48)");
        return comments;
    }

    @StringGetter(ALPHA)
    public double getAlpha() {
        return alpha;
    }
    @StringSetter(ALPHA)
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    @StringGetter(BETA)
    public double getBeta() {
        return beta;
    }
    @StringSetter(BETA)
    public void setBeta(double beta) {
        this.beta = beta;
    }

    @StringGetter(ETA)
    public double getEta() {
        return eta;
    }
    @StringSetter(ETA)
    public void setEta(double eta) {
        this.eta = eta;
    }

}
