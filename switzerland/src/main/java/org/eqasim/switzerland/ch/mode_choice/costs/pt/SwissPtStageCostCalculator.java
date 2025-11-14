package org.eqasim.switzerland.ch.mode_choice.costs.pt;

import java.util.HashMap;
import java.util.Map;

import org.eqasim.switzerland.ch.mode_choice.parameters.SwissPTAuthorityPricing;


public class SwissPtStageCostCalculator {

    public Map<String, PtStageCostCalculator> priceCalculators = new HashMap<>();

    public SwissPtStageCostCalculator(){
        DefaultDistanceBasedCalculator calculator = new DefaultDistanceBasedCalculator(0.19, 2.04, -0.00011);
        priceCalculators.put("None", calculator);

        SBBPtStageCostCalculator sbbCalculator = new SBBPtStageCostCalculator();
        priceCalculators.put("SBB", sbbCalculator);

        priceCalculators.put("Arcobaleno", SwissPTAuthorityPricing.ZonalAuthorityPricing.Arcobaleno);
        priceCalculators.put("Awelle", SwissPTAuthorityPricing.ZonalAuthorityPricing.Awelle);
        priceCalculators.put("CTJU", new DefaultDistanceBasedCalculator(0.19, 2.04, -0.00011));
        priceCalculators.put("Davos", SwissPTAuthorityPricing.ZonalAuthorityPricing.Davos);
        priceCalculators.put("EngadinMobil", SwissPTAuthorityPricing.ZonalAuthorityPricing.EngadinMobil);
        priceCalculators.put("Frimobil", SwissPTAuthorityPricing.ZonalAuthorityPricing.Frimobil);
        priceCalculators.put("Klosters", SwissPTAuthorityPricing.ZonalAuthorityPricing.Klosters);
        priceCalculators.put("Libero", SwissPTAuthorityPricing.ZonalAuthorityPricing.Libero);
        priceCalculators.put("Mobilis", SwissPTAuthorityPricing.ZonalAuthorityPricing.Mobilis);
        priceCalculators.put("OndeVerte", SwissPTAuthorityPricing.ZonalAuthorityPricing.OndeVerte);
        priceCalculators.put("Ostwind", SwissPTAuthorityPricing.ZonalAuthorityPricing.Ostwind);
        priceCalculators.put("Passepartout", SwissPTAuthorityPricing.ZonalAuthorityPricing.Passepartout);
        priceCalculators.put("SionPT", SwissPTAuthorityPricing.ZonalAuthorityPricing.SionPT);
        priceCalculators.put("TNW", SwissPTAuthorityPricing.ZonalAuthorityPricing.TNW);
        priceCalculators.put("TVSZ", SwissPTAuthorityPricing.ZonalAuthorityPricing.TVSZ);
        priceCalculators.put("Unireso", SwissPTAuthorityPricing.ZonalAuthorityPricing.Unireso);
        priceCalculators.put("ZVB", SwissPTAuthorityPricing.ZonalAuthorityPricing.ZVB);
        priceCalculators.put("ZPassOstwind", SwissPTAuthorityPricing.ZonalAuthorityPricing.ZPassOstwind);
        priceCalculators.put("ZPassAwelle", SwissPTAuthorityPricing.ZonalAuthorityPricing.ZPassAwelle);
        priceCalculators.put("ZPassSchwyzZug", SwissPTAuthorityPricing.ZonalAuthorityPricing.ZPassSchwyzZug);
        priceCalculators.put("ZVV", SwissPTAuthorityPricing.ZonalAuthorityPricing.ZVV);
    }
    
}
