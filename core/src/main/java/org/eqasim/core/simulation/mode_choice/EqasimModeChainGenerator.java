package org.eqasim.core.simulation.mode_choice;

import com.google.common.base.Verify;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGenerator;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.ModeChainGeneratorFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EqasimModeChainGenerator implements ModeChainGenerator {

    private final List<String> modes;
    private final int[] indices;
    private final int numberOfModes;
    private final long maximumAlternatives;
    private boolean hasNext;

    public EqasimModeChainGenerator(Collection<String> availableModes, int numberOfTrips) {
        Verify.verify(numberOfTrips > 0, "Number of trips must be greater than zero");
        Verify.verify(!availableModes.isEmpty(), "At least one mode is required");

        this.indices = new int[numberOfTrips];
        this.indices[0] = -1;
        for(int i = 1; i < numberOfTrips; i++) {
            this.indices[i] = 0;
        }
        this.modes = List.of(availableModes.toArray(new String[0]));
        this.numberOfModes = modes.size();
        this.maximumAlternatives = ArithmeticUtils.pow((long) numberOfModes, numberOfTrips);
        this.hasNext = true;
    }

    @Override
    public long getNumberOfAlternatives() {
        return this.maximumAlternatives;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public List<String> next() {
        Verify.verify(hasNext, "No more chains to generate");
        List<String> modes = new ArrayList<>(this.indices.length);
        this.indices[0]++;
        for(int i = 1; i < this.indices.length; i++) {
            int j = i-1;
            if(indices[j] == this.numberOfModes) {
                indices[j] = 0;
                indices[i]++;
                if(i == this.indices.length - 1 && indices[i] >= this.numberOfModes) {
                    throw new IllegalStateException("Illegal generator state");
                }
            } else if (indices[j] > this.numberOfModes) {
                throw new IllegalStateException("Illegal generator state");
            } else {
                break;
            }
        }
        hasNext = true;
        for(int i = 0; i < this.indices.length; i++) {
            hasNext &= this.indices[i] == this.numberOfModes - 1;
            modes.add(this.modes.get(i));
        }
        return modes;
    }

    public static class Factory implements ModeChainGeneratorFactory {

        @Override
        public ModeChainGenerator createModeChainGenerator(Collection<String> availableModes, Person person, List<DiscreteModeChoiceTrip> trips) {
            return new EqasimModeChainGenerator(availableModes, trips.size());
        }
    }
}
