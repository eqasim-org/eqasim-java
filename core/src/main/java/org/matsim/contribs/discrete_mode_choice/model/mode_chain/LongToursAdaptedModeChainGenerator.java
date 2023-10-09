package org.matsim.contribs.discrete_mode_choice.model.mode_chain;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LongToursAdaptedModeChainGenerator implements ModeChainGenerator {

    private final int numberOfTrips;
    private final int[] modeIndexPerTrip;
    private final List<String> availableModes;
    private final long numberOfAlternatives;
    private boolean haveNext;

    public LongToursAdaptedModeChainGenerator(Collection<String> availableModes, int numberOfTrips) {
        this.numberOfTrips = numberOfTrips;
        this.modeIndexPerTrip = new int[numberOfTrips];
        for(int i=0; i<numberOfTrips; i++) {
            this.modeIndexPerTrip[i] = 0;
        }
        this.availableModes = new ArrayList<>(availableModes);
        this.numberOfAlternatives = ArithmeticUtils.pow((long) availableModes.size(), numberOfTrips);
        this.haveNext = numberOfTrips > 0 && availableModes.size() > 0;
    }

    @Override
    public long getNumberOfAlternatives() {
        return this.numberOfAlternatives;
    }

    @Override
    public boolean hasNext() {
        return this.haveNext;
    }

    @Override
    public List<String> next() {
        List<String> chain = new ArrayList<>();
        int carry = 1;
        for(int i=0; i<numberOfTrips; i++) {
            chain.add(this.availableModes.get(this.modeIndexPerTrip[i]));
            this.modeIndexPerTrip[i] += carry;
            if (this.modeIndexPerTrip[i] == this.availableModes.size()) {
                this.modeIndexPerTrip[i] = 0;
            } else {
                carry = 0;
            }
        }
        this.haveNext = carry == 0;
        return chain;
    }



    static public class Factory implements ModeChainGeneratorFactory {
        @Override
        public ModeChainGenerator createModeChainGenerator(Collection<String> modes, Person person,
                                                           List<DiscreteModeChoiceTrip> trips) {
            return new DefaultModeChainGenerator(modes, trips.size());
        }
    }
}
