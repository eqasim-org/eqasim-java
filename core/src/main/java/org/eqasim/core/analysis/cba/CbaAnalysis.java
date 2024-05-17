package org.eqasim.core.analysis.cba;

import org.apache.poi.ss.usermodel.Sheet;
import org.eqasim.core.analysis.cba.analyzers.CbaAnalyzer;
import org.eqasim.core.analysis.cba.utils.ExtendedWorkbook;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CbaAnalysis {

    private final List<CbaAnalyzer> singleIterationCbaAnalyzers = new ArrayList<>();
    private final List<CbaAnalyzer> permanentCbaAnalyzers = new ArrayList<>();
    private final MatsimServices matsimServices;
    private final Network network;
    private final CbaConfigGroup configGroup;

    public CbaAnalysis(CbaConfigGroup configGroup, MatsimServices matsimServices, Network network) {
        this.configGroup = configGroup;
        this.matsimServices = matsimServices;
        this.network = network;
    }

    public void addSingleIterationAnalyzer(CbaAnalyzer analyzer) {
        this.singleIterationCbaAnalyzers.add(analyzer);
    }

    public void addPermanentAnalyzer(CbaAnalyzer analyzer) {
        this.permanentCbaAnalyzers.add(analyzer);
    }

    public void notifyIterationEnd(IterationEndsEvent event) {
        if(event.getIteration() % configGroup.getOutputFrequency() == 0) {
            try {
                ExtendedWorkbook workbook = new ExtendedWorkbook(500000);
                List<CbaAnalyzer> analyzers = new ArrayList<>(this.singleIterationCbaAnalyzers);
                analyzers.addAll(this.permanentCbaAnalyzers);
                for(CbaAnalyzer cbaAnalyzer : analyzers) {
                    String[] sheetsNames = cbaAnalyzer.getSheetsNames();
                    List<Sheet> sheets = new ArrayList<>();
                    for(String sheetName : sheetsNames) {
                        sheets.add(workbook.createSheet(sheetName));
                    }
                    cbaAnalyzer.fillSheets(sheets);
                }
                workbook.writeToFiles(this.matsimServices.getControlerIO().getIterationFilename(event.getIteration(), "cba"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.singleIterationCbaAnalyzers.clear();
    }
}
