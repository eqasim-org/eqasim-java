package org.eqasim.switzerland.ch.mode_choice.utilities.variables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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


    @SuppressWarnings("null")
    private Set<Authority> extractAuthorities(SwissPtLegVariables leg){
        if (leg.getZones().isEmpty()) {
            return Set.of(new Authority("None", 0, "Default"));
        }

        Set<Authority> authorities = new HashSet<>();
        Map<String, List<Zone>> legZones = leg.getZones();

        for (Map.Entry<String, List<Zone>> authZones : legZones.entrySet()){
            Zone zone = authZones.getValue().get(0);
            authorities.add(zone.getAuthority());
        }
        return authorities;
    }


    @SuppressWarnings("null")
    public Map<String, List<SwissPtLegVariables>> getPricingStrategy(){
        Map<String, List<SwissPtLegVariables>> groupedByAuthority = new HashMap<>();

        List<Set<Authority>> allAuthorities = new ArrayList<Set<Authority>>();
        Map<Authority, Integer> frequency   = new HashMap<>();
        
        for (SwissPtLegVariables currentLeg : this.legVariables){
            Set<Authority> authorities = extractAuthorities(currentLeg);
            allAuthorities.add(authorities);

            for (Authority auth : authorities) {
                frequency.merge(auth, 1, Integer::sum);
            }
        }

        int numLegs = allAuthorities.size();

        // Check if one authority covers all legs
        List<Authority> globalAuthorities = frequency.entrySet().stream()
            .filter(e -> e.getValue() == numLegs) // appears in every leg
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

         if (!globalAuthorities.isEmpty()) {
            // If multiple → pick the one with highest priority
            Authority chosenGlobal = globalAuthorities.stream()
                .max(Comparator.comparingInt(Authority::getPriority))
                .get();

            groupedByAuthority.put(chosenGlobal.getId(), legVariables);

            //System.out.println("   The following authority is common for all legs: " + chosenGlobal.getId());

            return groupedByAuthority;
        }

        // Assign legs to authorities
        for (int i = 0; i< legVariables.size(); i++){
            SwissPtLegVariables legVariable = legVariables.get(i);
            Set<Authority> authorities = allAuthorities.get(i);

            List<Authority> sharedWithOtherLegs = authorities.stream().filter(a -> frequency.getOrDefault(a, 0) >1).collect(Collectors.toList());

            Authority chosen;
            if (! sharedWithOtherLegs.isEmpty()){
                chosen = sharedWithOtherLegs.stream().min(Comparator.comparingInt(Authority::getPriority)).get();
            } else {
                chosen = authorities.stream().max(Comparator.comparingInt(Authority::getPriority)).orElseThrow(() -> new RuntimeException("No authority found"));
            }

            //System.out.println("   The following authority applies for the leg from " + legVariable.fromNodeName + " to " + legVariable.toNodeName + ": " + chosen.getId());
            groupedByAuthority.computeIfAbsent(chosen.getId(), k -> new ArrayList<>()).add(legVariable);
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