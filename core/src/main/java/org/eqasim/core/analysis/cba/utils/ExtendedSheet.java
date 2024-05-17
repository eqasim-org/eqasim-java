package org.eqasim.core.analysis.cba.utils;

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.*;

public class ExtendedSheet implements Sheet {

    private final List<XSSFSheet> sheets;
    private final ExtendedWorkbook workbook;


    public ExtendedSheet(ExtendedWorkbook workbook, List<XSSFSheet> sheets) {
        this.workbook = workbook;
        this.sheets = sheets;
    }

    public void addSheet(XSSFSheet sheet) {
        this.sheets.add(sheet);
    }

    @Override
    public Row createRow(int rownum) {
        int targetSheetIndex = rownum / this.workbook.getRowsLimit();
        if(targetSheetIndex >= this.sheets.size()) {
            this.workbook.requireSubSheet(targetSheetIndex);
        }
        return this.sheets.get(targetSheetIndex).createRow(rownum%this.workbook.getRowsLimit());
    }

    @Override
    public void removeRow(Row row) {

    }

    @Override
    public Row getRow(int rownum) {
        int targetSheet = rownum / this.workbook.getRowsLimit();
        return this.sheets.get(targetSheet).getRow(rownum%this.workbook.getRowsLimit());
    }

    @Override
    public int getPhysicalNumberOfRows() {
        int result = 0;
        for(Sheet sheet : this.sheets) {
            result += sheet.getPhysicalNumberOfRows();
        }
        return result;
    }

    @Override
    public int getFirstRowNum() {
        int i = 0;
        int indexInSheet = 0;
        while(i<this.sheets.size() && indexInSheet == 0) {
            indexInSheet = this.sheets.get(i).getFirstRowNum();
            i++;
        }
        if(indexInSheet > 0) {
            return i*this.workbook.getRowsLimit() + indexInSheet;
        }
        return 0;
    }

    @Override
    public int getLastRowNum() {
        int i = this.sheets.size() - 1;
        int indexInSheet = 0;
        while(i >= 0 && indexInSheet == 0) {
            indexInSheet = this.sheets.get(i).getLastRowNum();
            i--;
        }
        if(indexInSheet > 0) {
            return i*this.workbook.getRowsLimit() + indexInSheet;
        }
        return 0;
    }

    @Override
    public void setColumnHidden(int columnIndex, boolean hidden) {
        for(Sheet sheet: this.sheets) {
            sheet.setColumnHidden(columnIndex, hidden);
        }
    }

    @Override
    public boolean isColumnHidden(int columnIndex) {
        return this.sheets.get(0).isColumnHidden(columnIndex);
    }

    @Override
    public void setRightToLeft(boolean value) {
        for(Sheet sheet: this.sheets) {
            sheet.setRightToLeft(true);
        }
    }

    @Override
    public boolean isRightToLeft() {
        return this.sheets.get(0).isRightToLeft();
    }

    @Override
    public void setColumnWidth(int columnIndex, int width) {
        for(Sheet sheet: this.sheets) {
            sheet.setColumnWidth(columnIndex, width);
        }
    }

    @Override
    public int getColumnWidth(int columnIndex) {
        return this.sheets.get(0).getColumnWidth(columnIndex);
    }

    @Override
    public float getColumnWidthInPixels(int columnIndex) {
        return this.sheets.get(0).getColumnWidthInPixels(columnIndex);
    }

    @Override
    public void setDefaultColumnWidth(int width) {
        for(Sheet sheet: sheets){
            sheet.setDefaultColumnWidth(width);
        }
    }

    @Override
    public int getDefaultColumnWidth() {
        return this.sheets.get(0).getDefaultColumnWidth();
    }

    @Override
    public short getDefaultRowHeight() {
        return this.sheets.get(0).getDefaultRowHeight();
    }

    @Override
    public float getDefaultRowHeightInPoints() {
        return this.sheets.get(0).getDefaultRowHeightInPoints();
    }

    @Override
    public void setDefaultRowHeight(short height) {
        for(Sheet sheet: this.sheets) {
            sheet.setDefaultRowHeight(height);
        }
    }

    @Override
    public void setDefaultRowHeightInPoints(float height) {
        for(Sheet sheet: this.sheets) {
            sheet.setDefaultRowHeightInPoints(height);
        }
    }

    @Override
    public CellStyle getColumnStyle(int column) {
        return this.sheets.get(0).getColumnStyle(column);
    }

    @Override
    public int addMergedRegion(CellRangeAddress region) {
        throw new NotImplementedException("This operation is not yet implemented");
    }

    @Override
    public int addMergedRegionUnsafe(CellRangeAddress cellRangeAddress) {
        return 0;
    }

    @Override
    public void validateMergedRegions() {

    }

    @Override
    public void setVerticallyCenter(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setVerticallyCenter(value);
        }
    }

    @Override
    public void setHorizontallyCenter(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setHorizontallyCenter(value);
        }
    }

    @Override
    public boolean getHorizontallyCenter() {
        return this.sheets.get(0).getHorizontallyCenter();
    }

    @Override
    public boolean getVerticallyCenter() {
        return this.sheets.get(0).getVerticallyCenter();
    }

    @Override
    public void removeMergedRegion(int index) {
        throw new NotImplementedException("This operation is not yet implemented");
    }

    @Override
    public void removeMergedRegions(Collection<Integer> collection) {

    }

    @Override
    public int getNumMergedRegions() {
        return 0;
    }

    @Override
    public CellRangeAddress getMergedRegion(int index) {
        return null;
    }

    @Override
    public List<CellRangeAddress> getMergedRegions() {
        return null;
    }

    @Override
    public Iterator<Row> rowIterator() {
        List<Iterator<Row>> iterators = new ArrayList<>();
        for(Sheet sheet: sheets) {
            iterators.add(sheet.rowIterator());
        }
        return new IteratorChain(iterators);
    }

    @Override
    public void setForceFormulaRecalculation(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setForceFormulaRecalculation(value);
        }
    }

    @Override
    public boolean getForceFormulaRecalculation() {
        return this.sheets.get(0).getForceFormulaRecalculation();
    }

    @Override
    public void setAutobreaks(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setAutobreaks(value);
        }
    }

    @Override
    public void setDisplayGuts(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setDisplayGuts(value);
        }
    }

    @Override
    public void setDisplayZeros(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setDisplayZeros(value);
        }
    }

    @Override
    public boolean isDisplayZeros() {
        return this.sheets.get(0).isDisplayZeros();
    }

    @Override
    public void setFitToPage(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setFitToPage(value);
        }
    }

    @Override
    public void setRowSumsBelow(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setRowSumsBelow(value);
        }
    }

    @Override
    public void setRowSumsRight(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setRowSumsRight(value);
        }
    }

    @Override
    public boolean getAutobreaks() {
        return this.sheets.get(0).getAutobreaks();
    }

    @Override
    public boolean getDisplayGuts() {
        return this.sheets.get(0).getDisplayGuts();
    }

    @Override
    public boolean getFitToPage() {
        return this.sheets.get(0).getFitToPage();
    }

    @Override
    public boolean getRowSumsBelow() {
        return this.sheets.get(0).getRowSumsBelow();
    }

    @Override
    public boolean getRowSumsRight() {
        return this.sheets.get(0).getRowSumsRight();
    }

    @Override
    public boolean isPrintGridlines() {
        return this.sheets.get(0).isPrintGridlines();
    }

    @Override
    public void setPrintGridlines(boolean show) {
        for(Sheet sheet: sheets) {
            sheet.setPrintGridlines(show);
        }
    }

    @Override
    public boolean isPrintRowAndColumnHeadings() {
        return false;
    }

    @Override
    public void setPrintRowAndColumnHeadings(boolean b) {

    }

    @Override
    public PrintSetup getPrintSetup() {
        return this.sheets.get(0).getPrintSetup();
    }

    @Override
    public Header getHeader() {
        return this.sheets.get(0).getHeader();
    }

    @Override
    public Footer getFooter() {
        return this.sheets.get(this.sheets.size()-1).getFooter();
    }

    @Override
    public void setSelected(boolean value) {
        for(Sheet sheet: sheets) {
            sheet.setSelected(value);
        }
    }

    @Override
    public double getMargin(short margin) {
        return this.sheets.get(0).getMargin(margin);
    }

    @Override
    public double getMargin(PageMargin pageMargin) {
        throw new NotImplementedException();
    }

    @Override
    public void setMargin(short margin, double size) {
        for(Sheet sheet: sheets) {
            sheet.setMargin(margin, size);
        }
    }

    @Override
    public void setMargin(PageMargin pageMargin, double v) {

    }

    @Override
    public boolean getProtect() {
        return this.sheets.get(0).getProtect();
    }

    @Override
    public void protectSheet(String password) {
        for(Sheet sheet: sheets) {
            sheet.protectSheet(password);
        }
    }

    @Override
    public boolean getScenarioProtect() {
        return this.sheets.get(0).getScenarioProtect();
    }

    @Override
    public void setZoom(int i) {

    }

    @Override
    public short getTopRow() {
        return this.sheets.get(0).getTopRow();
    }

    @Override
    public short getLeftCol() {
        return this.sheets.get(0).getLeftCol();
    }

    @Override
    public void showInPane(int toprow, int leftcol) {
        throw new NotImplementedException();
    }

    @Override
    public void shiftRows(int startRow, int endRow, int n) {
        throw new NotImplementedException();
    }

    @Override
    public void shiftRows(int startRow, int endRow, int n, boolean copyRowHeight, boolean resetOriginalRowHeight) {
        throw new NotImplementedException();
    }

    @Override
    public void shiftColumns(int i, int i1, int i2) {

    }

    @Override
    public void createFreezePane(int colSplit, int rowSplit, int leftmostColumn, int topRow) {
        throw new NotImplementedException();
    }

    @Override
    public void createFreezePane(int colSplit, int rowSplit) {
        throw new NotImplementedException();
    }

    @Override
    public void createSplitPane(int xSplitPos, int ySplitPos, int leftmostColumn, int topRow, int activePane) {
        throw new NotImplementedException();
    }

    @Override
    public void createSplitPane(int i, int i1, int i2, int i3, PaneType paneType) {

    }

    @Override
    public PaneInformation getPaneInformation() {
        throw new NotImplementedException();
    }

    @Override
    public void setDisplayGridlines(boolean show) {
        for(Sheet sheet: sheets) {
            sheet.setDisplayGridlines(show);
        }
    }

    @Override
    public boolean isDisplayGridlines() {
        return this.sheets.get(0).isDisplayGridlines();
    }

    @Override
    public void setDisplayFormulas(boolean show) {
        for(Sheet sheet: sheets) {
            sheet.setDisplayFormulas(show);
        }
    }

    @Override
    public boolean isDisplayFormulas() {
        return false;
    }

    @Override
    public void setDisplayRowColHeadings(boolean show) {
        for(Sheet sheet: sheets) {
            sheet.setDisplayRowColHeadings(show);
        }
    }

    @Override
    public boolean isDisplayRowColHeadings() {
        return this.sheets.get(0).isDisplayRowColHeadings();
    }

    @Override
    public void setRowBreak(int row) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isRowBroken(int row) {
        throw new NotImplementedException();
    }

    @Override
    public void removeRowBreak(int row) {
        throw new NotImplementedException();
    }

    @Override
    public int[] getRowBreaks() {
        throw new NotImplementedException();
    }

    @Override
    public int[] getColumnBreaks() {
        throw new NotImplementedException();
    }

    @Override
    public void setColumnBreak(int column) {
        for(Sheet sheet: this.sheets) {
            sheet.setColumnBreak(column);
        }
    }

    @Override
    public boolean isColumnBroken(int column) {
        return this.sheets.get(0).isColumnBroken(column);
    }

    @Override
    public void removeColumnBreak(int column) {
        for(Sheet sheet: this.sheets) {
            sheet.removeColumnBreak(column);
        }
    }

    @Override
    public void setColumnGroupCollapsed(int columnNumber, boolean collapsed) {
        for(Sheet sheet: this.sheets) {
            sheet.setColumnGroupCollapsed(columnNumber, collapsed);
        }
    }

    @Override
    public void groupColumn(int fromColumn, int toColumn) {
        for(Sheet sheet: this.sheets) {
            sheet.groupColumn(fromColumn, toColumn);
        }
    }

    @Override
    public void ungroupColumn(int fromColumn, int toColumn) {
        for(Sheet sheet: this.sheets) {
            sheet.ungroupColumn(fromColumn, toColumn);
        }
    }

    @Override
    public void groupRow(int fromRow, int toRow) {
        throw new NotImplementedException();
    }

    @Override
    public void ungroupRow(int fromRow, int toRow) {
        throw new NotImplementedException();
    }

    @Override
    public void setRowGroupCollapsed(int row, boolean collapse) {
        throw new NotImplementedException();
    }

    @Override
    public void setDefaultColumnStyle(int column, CellStyle style) {
        for(Sheet sheet: this.sheets) {
            sheet.setColumnBreak(column);
        }
    }

    @Override
    public void autoSizeColumn(int column) {
        for(Sheet sheet: this.sheets) {
            sheet.autoSizeColumn(column);
        }
    }

    @Override
    public void autoSizeColumn(int column, boolean useMergedCells) {
        for(Sheet sheet: this.sheets) {
            sheet.autoSizeColumn(column, useMergedCells);
        }
    }

    @Override
    public Comment getCellComment(CellAddress cellAddress) {
        return null;
    }

    @Override
    public Map<CellAddress, ? extends Comment> getCellComments() {
        return null;
    }

    @Override
    public Drawing<?> getDrawingPatriarch() {
        return null;
    }

    @Override
    public Drawing createDrawingPatriarch() {
        throw new NotImplementedException();
    }

    @Override
    public Workbook getWorkbook() {
        return this.workbook;
    }

    @Override
    public String getSheetName() {
        return this.sheets.get(0).getSheetName();
    }

    @Override
    public boolean isSelected() {
        return this.sheets.get(0).isSelected();
    }

    @Override
    public CellRange<? extends Cell> setArrayFormula(String formula, CellRangeAddress range) {
        throw new NotImplementedException();
    }

    @Override
    public CellRange<? extends Cell> removeArrayFormula(Cell cell) {
        throw new NotImplementedException();
    }

    @Override
    public DataValidationHelper getDataValidationHelper() {
        throw new NotImplementedException();
    }

    @Override
    public List<? extends DataValidation> getDataValidations() {
        throw new NotImplementedException();
    }

    @Override
    public void addValidationData(DataValidation dataValidation) {
        throw new NotImplementedException();
    }

    @Override
    public AutoFilter setAutoFilter(CellRangeAddress range) {
        throw new NotImplementedException();
    }

    @Override
    public SheetConditionalFormatting getSheetConditionalFormatting() {
        throw new NotImplementedException();
    }

    @Override
    public CellRangeAddress getRepeatingRows() {
        throw new NotImplementedException();
    }

    @Override
    public CellRangeAddress getRepeatingColumns() {
        throw new NotImplementedException();
    }

    @Override
    public void setRepeatingRows(CellRangeAddress rowRangeRef) {
        throw new NotImplementedException();
    }

    @Override
    public void setRepeatingColumns(CellRangeAddress columnRangeRef) {
        throw new NotImplementedException();
    }

    @Override
    public int getColumnOutlineLevel(int columnIndex) {
        throw new NotImplementedException();
    }

    @Override
    public Hyperlink getHyperlink(int i, int i1) {
        return null;
    }

    @Override
    public Hyperlink getHyperlink(CellAddress cellAddress) {
        return null;
    }

    @Override
    public List<? extends Hyperlink> getHyperlinkList() {
        return null;
    }

    @Override
    public CellAddress getActiveCell() {
        return null;
    }

    @Override
    public void setActiveCell(CellAddress cellAddress) {

    }

    @Override
    public Iterator<Row> iterator() {
        return this.rowIterator();
    }
}
