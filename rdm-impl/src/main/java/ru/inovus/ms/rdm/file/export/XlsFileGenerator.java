package ru.inovus.ms.rdm.file.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by znurgaliev on 24.07.2018.
 */
public class XlsFileGenerator extends PerRowFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(XlsFileGenerator.class);

    private Map<String, Integer> fieldColumn = new HashMap<>();
    private SXSSFWorkbook workbook;
    private int pageSize = 500;
    private CellStileFactory stileFactory;

    public XlsFileGenerator(Iterator<Row> rowIterator) {
        super(rowIterator);
    }

    public XlsFileGenerator(Iterator<Row> rowIterator, Structure structure) {
        super(rowIterator, structure);
    }

    public XlsFileGenerator(Iterator<Row> rowIterator, int pageSize) {
        super(rowIterator);
        this.pageSize = pageSize;
    }

    public XlsFileGenerator(Iterator<Row> rowIterator, Structure structure, int pageSize) {
        super(rowIterator, structure);
        this.pageSize = pageSize;
    }

    @Override
    protected void startWrite() {
        logger.info("Start generate XLSX");
        workbook = new SXSSFWorkbook(500);
        stileFactory = new CellStileFactory();
        SXSSFSheet activeSheet = workbook.createSheet("Страница 1");
        createFirstRow(activeSheet);
    }

    @Override
    protected void write(Row row) {
        org.apache.poi.ss.usermodel.Row wbRow = getNextRow();
        row.getData().entrySet()
                .forEach(entry -> fillCell(wbRow.createCell(getColumnIndex(entry.getKey())), entry.getValue()));
    }

    @Override
    protected void endWrite() {
        OutputStream ncos = new NoCloseOutputStreamWrapper(getOutputStream());
        try {
            autoSizeAllSheet();
            workbook.write(ncos);
            workbook.close();
            ncos.flush();
            fieldColumn.clear();
            stileFactory = null;
            logger.info("XLSX generate finished");
        } catch (IOException e) {
            logger.error("cannot generate XLSX", e);
            throw new RdmException("cannot generate XLSX");
        }
    }

    private org.apache.poi.ss.usermodel.Row getNextRow() {
        Sheet activeSheet = getActiveSheet();
        return activeSheet.createRow(activeSheet.getLastRowNum() + 1);
    }

    private SXSSFSheet getActiveSheet() {
        int sheetIndex = workbook.getActiveSheetIndex();
        SXSSFSheet activeSheet = workbook.getSheetAt(sheetIndex);
        if (activeSheet.getPhysicalNumberOfRows() >= pageSize) {
            activeSheet = workbook.createSheet("Страница " + (sheetIndex + 2));
            workbook.setActiveSheet(sheetIndex + 1);
            fieldColumn.clear();
            createFirstRow(activeSheet);
        }
        return activeSheet;
    }

    private SXSSFRow getActiveFirstRow() {
        SXSSFSheet activeSheet = getActiveSheet();
        if (activeSheet.getPhysicalNumberOfRows() > 0) {
            return activeSheet.getRow(activeSheet.getFirstRowNum());
        } else return createFirstRow(activeSheet);
    }

    private SXSSFRow createFirstRow(SXSSFSheet sheet) {
        SXSSFRow row = sheet.createRow(0);
        if (getStructure() != null)
            getStructure().getAttributes().forEach(a -> getColumnIndex(a.getCode()));
        return row;
    }

    private int getColumnIndex(String fieldName) {
        Integer columnIndex = fieldColumn.get(fieldName);
        if (columnIndex == null) {
            columnIndex = createColumn(fieldName);
        }
        return columnIndex;
    }

    private int createColumn(String fieldName) {

        Integer columnIndex = fieldColumn.size();

        SXSSFCell currrentCell = getActiveFirstRow().createCell(columnIndex);
        currrentCell.setCellStyle(stileFactory.getFirstRowStyle());
        currrentCell.setCellValue(fieldName);
        currrentCell.getSheet().trackColumnForAutoSizing(columnIndex);

        fieldColumn.put(fieldName, columnIndex);

        return columnIndex;
    }

    private void fillCell(Cell cell, Object value) {

        if (value instanceof LocalDate) {
            Date date = Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant());
            cell.setCellStyle(stileFactory.getDateStyle());
            cell.setCellValue(date);
        } else if (value instanceof Boolean) {
            cell.setCellStyle(stileFactory.getDefaultStyle());
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Number) {
            cell.setCellStyle(stileFactory.getDefaultStyle());
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Reference) {
            cell.setCellStyle(stileFactory.getDefaultStyle());
            cell.setCellValue(((Reference) value).getValue());
        } else {
            cell.setCellStyle(stileFactory.getDefaultStyle());
            cell.setCellValue(Optional.ofNullable(value).orElse("").toString());
        }

    }

    private void autoSizeAllSheet() {
        for (int i = 0; i <= workbook.getActiveSheetIndex(); i++) {
            SXSSFSheet sheet = workbook.getSheetAt(i);
            sheet.getTrackedColumnsForAutoSizing()
                    .forEach(sheet::autoSizeColumn);
        }
    }

    @Override
    public void close() throws IOException {
        if (workbook != null)
            workbook.close();
    }


    private class NoCloseOutputStreamWrapper extends BufferedOutputStream {

        public NoCloseOutputStreamWrapper(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            //disable close for outputStream
        }
    }

    private class CellStileFactory {

        private static final String XLSX_DATE_FORMAT = "dd.MM.yyyy";

        private CellStyle firstRowStile;
        private CellStyle defaultStyle;
        private CellStyle dateStyle;
        private Font defaultFont;

        private CellStyle getFirstRowStyle() {
            if (firstRowStile == null) {
                Font nameFont = workbook.createFont();
                nameFont.setFontHeightInPoints((short) 12);
                nameFont.setFontName("Times New Roman");
                nameFont.setBold(true);
                firstRowStile = workbook.createCellStyle();
                firstRowStile.setFont(nameFont);
            }
            return firstRowStile;
        }

        private CellStyle getDefaultStyle() {
            if (defaultStyle == null) {
                defaultStyle = workbook.createCellStyle();
                defaultStyle.setFont(getDefaultFont());

            }
            return defaultStyle;
        }

        private CellStyle getDateStyle() {
            if (dateStyle == null) {
                dateStyle = workbook.createCellStyle();
                dateStyle.setFont(getDefaultFont());
                dateStyle.setDataFormat(workbook.createDataFormat().getFormat(XLSX_DATE_FORMAT));
            }
            return dateStyle;
        }

        private Font getDefaultFont() {
            if (defaultFont == null) {
                defaultFont = workbook.createFont();
                defaultFont.setFontHeightInPoints((short) 12);
                defaultFont.setFontName("Times New Roman");
            }
            return defaultFont;
        }
    }
}
