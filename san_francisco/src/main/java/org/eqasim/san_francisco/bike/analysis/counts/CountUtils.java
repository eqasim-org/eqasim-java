package org.eqasim.san_francisco.bike.analysis.counts;

import org.eqasim.san_francisco.bike.analysis.counts.items.CountItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.LinkedList;
import java.util.List;

public class CountUtils {

    public static List<CountItem> createNewCountItemsList(Id<Link> linkId, String mode, int numberBins, double binSize) {
        List<CountItem> countItems = new LinkedList<>();
        for (int i = 0; i < numberBins; i++) {
            countItems.add(i, new CountItem(linkId, mode, i * binSize, 0));
        }
        return countItems;
    }

    public static int getTimeBin(double time, int numberBins, double binSize) {
        return ((int) (time / binSize)) % numberBins;
    }
}
