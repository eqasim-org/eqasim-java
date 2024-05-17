package org.eqasim.core.analysis.cba.analyzers.agentsAnalysis;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.eqasim.core.analysis.cba.analyzers.CbaAnalyzer;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.MobsimScopeEventHandler;

import java.util.List;

public class AgentsAnalyzer implements CbaAnalyzer, MobsimScopeEventHandler, IterationEndsListener {

    private static final String[] HEADERS = new String[]{"personId", "score"};

    private final AgentsAnalyzerConfigGroup configGroup;
    private final Population population;


    public AgentsAnalyzer(AgentsAnalyzerConfigGroup configGroup, Population population) {
        this.configGroup = configGroup;
        this.population = population;
    }

    @Override
    public String[] getSheetsNames() {
        return new String[]{configGroup.getScoresSheetName()};
    }

    @Override
    public void fillSheets(List<Sheet> sheets) {
        assert sheets.size() >= 1;
        Sheet sheet = sheets.get(0);
        assert sheet.getSheetName().equals(configGroup.getScoresSheetName());

        Row row = sheet.createRow(0);
        for (int i = 0; i< HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADERS[i]);
        }

        int rowCount = 1;
        for(Person person : population.getPersons().values()) {
            row = sheet.createRow(rowCount);
            Cell cell = row.createCell(0);
            cell.setCellValue(person.getId().toString());
            cell = row.createCell(1);
            cell.setCellValue(person.getSelectedPlan().getScore());
            rowCount+=1;
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {

    }
}
