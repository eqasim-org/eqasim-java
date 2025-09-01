package org.eqasim.switzerland.ch.mode_choice.utilities.variables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Authority;
import org.eqasim.switzerland.ch.utils.pricing.inputs.Zone;

public class SwissPtVariables implements BaseVariables  {
    
	public List<SwissPtLegVariables> legVariables;

    public SwissPtVariables(){
        this.legVariables = new ArrayList<SwissPtLegVariables>();        
    }

    public void addStage(SwissPtLegVariables newLegVariables){
        this.legVariables.add( newLegVariables);
    }


    private Set<Authority> extractAuthority(SwissPtLegVariables leg){
        if (leg.getZones().isEmpty()) {
            return Set.of(new Authority("None", 0, "Default"));
        }

        Set<Authority> authorities = new HashSet<>();
        for (Zone z: leg.getZones()){
            authorities.add(z.getAuthority());
        }
        return authorities;
    }


    public Map<String, List<SwissPtLegVariables>> getPricingStrategy(){
        Map<String, List<SwissPtLegVariables>> groupedByAuthority = new HashMap<>();
        
        for (SwissPtLegVariables currentLeg : this.legVariables){
            Set<Authority> authorities = extractAuthority(currentLeg);
            for (Authority authority : authorities){
                groupedByAuthority.computeIfAbsent(authority.getId(), k -> new ArrayList<SwissPtLegVariables>()).add(currentLeg);
            }
        }

        return groupedByAuthority;
        
    }


    @Override
    public String toString(){
        String result = "";
        for (SwissPtLegVariables legInfo : this.legVariables){
            result = result + legInfo.toString() + "\n";
        }
        return result;
    }
    
}