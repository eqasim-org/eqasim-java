package org.eqasim.core.analysis.cba;

import org.eqasim.core.analysis.cba.analyzers.drtAnalysis.DrtAnalyzer;
import org.eqasim.core.analysis.cba.analyzers.ptAnalysis.PtAnalyzer;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * TODO :
 *  Number of trips in the excel file
 *  Travelled vehicle kilometers (the distance travelled by vehicles & not passengers) in another sheet, along with travelled distance
 *  another sheet with score per person
 *  Listen for iteration ends and check the population, it will contain the updated scores
 */

public class CbaControlerListener implements IterationEndsListener {

    private final MatsimServices matsimServices;
    private Network network;
    private final DrtAnalyzer drtAnalyzer;
    private final PtAnalyzer ptAnalyzer;
    private final CbaAnalysis cbaAnalysis;

    public CbaControlerListener(CbaAnalysis cbaAnalysis, MatsimServices matsimServices, Network network, DrtAnalyzer drtAnalyzer, PtAnalyzer ptAnalyzer) {
        this.matsimServices = matsimServices;
        this.network = network;
        this.drtAnalyzer = drtAnalyzer;
        this.ptAnalyzer = ptAnalyzer;
        this.cbaAnalysis = cbaAnalysis;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        cbaAnalysis.notifyIterationEnd(event);
    }
}
