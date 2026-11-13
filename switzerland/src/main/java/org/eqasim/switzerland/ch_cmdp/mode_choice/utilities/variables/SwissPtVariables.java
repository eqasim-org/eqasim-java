package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.core.simulation.mode_choice.utilities.variables.BaseVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;

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


    /**
     * Which network prices which legs, one ticket each (group_rides in eqasim-switzerland, utils/gtfs_router/pricing.py).
     *
     * If one network covers all the legs, it prices the whole trip (the one of the highest priority if there are
     * several). Otherwise only the networks of the highest priority of each leg count: each leg goes to one it shares
     * with another leg (the one of the lowest priority), or else to its own (the one of the highest priority). Ties are
     * broken by the network id, so that the tickets never depend on the order of a hash set.
     */
    public Map<String, List<SwissPtLegVariables>> getPricingStrategy(){
        Map<String, List<SwissPtLegVariables>> groupedByAuthority = new HashMap<>();
        if (this.legVariables.isEmpty()) {
            return groupedByAuthority;
        }

        Comparator<Authority> byPriority = Comparator.comparingInt(Authority::getPriority).thenComparing(Authority::getId);

        List<Set<Authority>> allAuthorities = new ArrayList<>();
        for (SwissPtLegVariables currentLeg : this.legVariables){
            allAuthorities.add(extractAuthorities(currentLeg));
        }

        // One network for all the legs: the one of the highest priority
        Set<Authority> everywhere = new HashSet<>(allAuthorities.get(0));
        for (Set<Authority> authorities : allAuthorities) {
            everywhere.retainAll(authorities);
        }

        if (!everywhere.isEmpty()) {
            groupedByAuthority.put(everywhere.stream().max(byPriority).get().getId(), this.legVariables);
            return groupedByAuthority;
        }

        // Otherwise, only the networks of the highest priority of each leg
        List<Set<Authority>> highest = new ArrayList<>();
        Map<Authority, Integer> frequency = new HashMap<>();
        for (Set<Authority> authorities : allAuthorities) {
            int top = authorities.stream().mapToInt(Authority::getPriority).max().getAsInt();
            Set<Authority> kept = authorities.stream().filter(a -> a.getPriority() == top).collect(Collectors.toSet());
            highest.add(kept);
            for (Authority auth : kept) {
                frequency.merge(auth, 1, Integer::sum);
            }
        }

        for (int i = 0; i < legVariables.size(); i++){
            Set<Authority> authorities = highest.get(i);
            List<Authority> sharedWithOtherLegs = authorities.stream().filter(a -> frequency.getOrDefault(a, 0) > 1).collect(Collectors.toList());

            Authority chosen = sharedWithOtherLegs.isEmpty()
                ? authorities.stream().max(byPriority).orElseThrow(() -> new RuntimeException("No authority found"))
                : sharedWithOtherLegs.stream().min(byPriority).get();

            groupedByAuthority.computeIfAbsent(chosen.getId(), k -> new ArrayList<>()).add(legVariables.get(i));
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