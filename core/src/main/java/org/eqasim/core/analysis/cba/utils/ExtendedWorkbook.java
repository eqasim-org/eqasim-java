package org.eqasim.core.analysis.cba.utils;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.formula.EvaluationWorkbook;
import org.apache.poi.ss.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExtendedWorkbook implements Workbook {
    private final List<XSSFWorkbook> workbooks;
    private final List<ExtendedSheet> extendedSheets;
    private final int rowsLimit;

    public ExtendedWorkbook(int rowsLimit) {
        this.workbooks = new ArrayList<>();
        this.workbooks.add(new XSSFWorkbook());
        this.extendedSheets = new ArrayList<>();
        this.rowsLimit = rowsLimit;
    }

    public int getRowsLimit(){
        return this.rowsLimit;
    }

    @Override
    public int getActiveSheetIndex() {
        return this.workbooks.get(0).getActiveSheetIndex();
    }

    @Override
    public void setActiveSheet(int sheetIndex) {
        for(Workbook workbook: this.workbooks) {
            workbook.setActiveSheet(sheetIndex);
        }
    }

    @Override
    public int getFirstVisibleTab() {
        return this.workbooks.get(0).getFirstVisibleTab();
    }

    @Override
    public void setFirstVisibleTab(int sheetIndex) {
        for(Workbook workbook: this.workbooks) {
            workbook.setFirstVisibleTab(sheetIndex);
        }
    }

    @Override
    public void setSheetOrder(String sheetname, int pos) {
        for(Workbook workbook: this.workbooks) {
            workbook.setSheetOrder(sheetname, pos);
        }
    }

    @Override
    public void setSelectedTab(int index) {
        for(Workbook workbook: this.workbooks) {
            workbook.setSelectedTab(index);
        }
    }

    @Override
    public void setSheetName(int sheet, String name) {
        for(Workbook workbook: this.workbooks) {
            workbook.setSheetName(sheet, name);
        }
    }

    @Override
    public String getSheetName(int sheet) {
        return workbooks.get(0).getSheetName(sheet);
    }

    @Override
    public int getSheetIndex(String name) {
        return workbooks.get(0).getSheetIndex(name);
    }

    @Override
    public int getSheetIndex(Sheet sheet) {
        throw new NotImplementedException();
    }

    @Override
    public Sheet createSheet() {
        return null;
    }

    @Override
    public ExtendedSheet createSheet(String sheetName){
        List<XSSFSheet> sheets = new ArrayList<>();
        for(XSSFWorkbook workbook : workbooks) {
            sheets.add(workbook.createSheet(sheetName));
        }
        ExtendedSheet sheet = new ExtendedSheet(this, sheets);
        this.extendedSheets.add(sheet);
        return sheet;
    }

    @Override
    public Sheet cloneSheet(int sheetNum) {
        List<XSSFSheet> sheets = new ArrayList<>();
        for(XSSFWorkbook workbook : workbooks) {
            sheets.add(workbook.cloneSheet(sheetNum));
        }
        ExtendedSheet sheet = new ExtendedSheet(this, sheets);
        this.extendedSheets.add(sheet);
        return sheet;
    }

    @Override
    public Iterator<Sheet> sheetIterator() {
        return null;
    }

    @Override
    public int getNumberOfSheets() {
        return this.workbooks.get(0).getNumberOfSheets();
    }

    @Override
    public Sheet getSheetAt(int index) {
        throw new NotImplementedException();
    }

    @Override
    public Sheet getSheet(String name) {
        for(ExtendedSheet sheet: this.extendedSheets) {
            if(sheet.getSheetName().equals(name)) {
                return sheet;
            }
        }
        return null;
    }

    public void requireSubSheet(int newWorkbookIndex) {
        while(this.workbooks.size() <= newWorkbookIndex) {
            XSSFWorkbook workbook = new XSSFWorkbook();
            this.workbooks.add(workbook);
            for(int i=0; i<this.workbooks.get(0).getNumberOfSheets(); i++) {
                XSSFSheet sheet = this.workbooks.get(0).getSheetAt(i);
                XSSFSheet newSheet = workbook.createSheet(sheet.getSheetName());
                ExtendedSheet extendedSheet = (ExtendedSheet) this.getSheet(sheet.getSheetName());
                extendedSheet.addSheet(newSheet);
            }
        }
    }

    @Override
    public void removeSheetAt(int index) {
        throw new NotImplementedException();
    }

    @Override
    public Font createFont() {
        throw new NotImplementedException();
    }

    @Override
    public Font findFont(boolean b, short i, short i1, String s, boolean b1, boolean b2, short i2, byte b3) {
        return null;
    }


    @Override
    public int getNumberOfFonts() {
        throw new NotImplementedException();
    }

    @Override
    public int getNumberOfFontsAsInt() {
        return 0;
    }

    @Override
    public Font getFontAt(int i) {
        return null;
    }

    @Override
    public CellStyle createCellStyle() {
        throw new NotImplementedException();
    }

    @Override
    public int getNumCellStyles() {
        throw new NotImplementedException();
    }

    @Override
    public CellStyle getCellStyleAt(int i) {
        return null;
    }

    @Override
    public void write(OutputStream stream) throws IOException {

    }

    public void writeToFiles(String firstFileNameWithoutExtension) throws IOException {
        for(int i=0; i<workbooks.size(); i++) {
            FileOutputStream stream = new FileOutputStream(firstFileNameWithoutExtension+i+".xls");
            this.workbooks.get(i).write(stream);
        }
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public int getNumberOfNames() {
        throw new NotImplementedException();
    }

    @Override
    public Name getName(String name) {
        throw new NotImplementedException();
    }

    @Override
    public List<? extends Name> getNames(String s) {
        return null;
    }

    @Override
    public List<? extends Name> getAllNames() {
        return null;
    }

    @Override
    public Name createName() {
        throw new NotImplementedException();
    }

    @Override
    public void removeName(Name name) {

    }

    @Override
    public int linkExternalWorkbook(String name, Workbook workbook) {
        throw new NotImplementedException();
    }

    @Override
    public void setPrintArea(int sheetIndex, String reference) {
        throw new NotImplementedException();
    }

    @Override
    public void setPrintArea(int sheetIndex, int startColumn, int endColumn, int startRow, int endRow) {
        throw new NotImplementedException();
    }

    @Override
    public String getPrintArea(int sheetIndex) {
        throw new NotImplementedException();
    }

    @Override
    public void removePrintArea(int sheetIndex) {
        throw new NotImplementedException();
    }

    @Override
    public Row.MissingCellPolicy getMissingCellPolicy() {
        throw new NotImplementedException();
    }

    @Override
    public void setMissingCellPolicy(Row.MissingCellPolicy missingCellPolicy) {
        throw new NotImplementedException();
    }

    @Override
    public DataFormat createDataFormat() {
        throw new NotImplementedException();
    }

    @Override
    public int addPicture(byte[] pictureData, int format) {
        throw new NotImplementedException();
    }

    @Override
    public List<? extends PictureData> getAllPictures() {
        throw new NotImplementedException();
    }

    @Override
    public CreationHelper getCreationHelper() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isHidden() {
        throw new NotImplementedException();
    }

    @Override
    public void setHidden(boolean hiddenFlag) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isSheetHidden(int sheetIx) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isSheetVeryHidden(int sheetIx) {
        throw new NotImplementedException();
    }

    @Override
    public void setSheetHidden(int sheetIx, boolean hidden) {
        throw new NotImplementedException();
    }

    @Override
    public SheetVisibility getSheetVisibility(int i) {
        return null;
    }

    @Override
    public void setSheetVisibility(int i, SheetVisibility sheetVisibility) {

    }

    @Override
    public void addToolPack(UDFFinder toopack) {
        throw new NotImplementedException();
    }

    @Override
    public void setForceFormulaRecalculation(boolean value) {
        throw new NotImplementedException();
    }

    @Override
    public boolean getForceFormulaRecalculation() {
        throw new NotImplementedException();
    }

    @Override
    public SpreadsheetVersion getSpreadsheetVersion() {
        return null;
    }

    @Override
    public int addOlePackage(byte[] bytes, String s, String s1, String s2) throws IOException {
        return 0;
    }

    @Override
    public EvaluationWorkbook createEvaluationWorkbook() {
        return null;
    }

    @Override
    public CellReferenceType getCellReferenceType() {
        throw new NotImplementedException();
    }

    @Override
    public void setCellReferenceType(CellReferenceType cellReferenceType) {
        throw new NotImplementedException();
    }

}
