package org.eqasim.core.simulation.modes.parking_aware_car.utils;

import com.google.common.base.Verify;

import java.util.Comparator;

public class HierarchicalComparator<T> implements Comparator<T> {

    private final Comparator<T>[] comparators;

    public HierarchicalComparator(Comparator<T>... comparators) {
        this.comparators = comparators;
        Verify.verify(comparators.length > 0);
    }

    @Override
    public int compare(T o1, T o2) {
        for(Comparator<T> comparator : comparators) {
            int result = comparator.compare(o1, o2);
            if(result != 0) return result;
        }
        return 0;
    }
}
