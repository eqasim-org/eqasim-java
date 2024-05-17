package org.eqasim.core.analysis.cba.analyzers;

import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

public abstract class AbstractCBAAnalyzer implements CbaAnalyzer{
    @Override
    abstract public String[] getSheetsNames();

    @Override
    abstract public void fillSheets(List<Sheet> sheets);
}
