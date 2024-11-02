package ru.i_novus.ms.rdm.impl.file.export;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static ru.i_novus.ms.rdm.impl.util.XlsxUtil.XLSX_DATE_FORMAT;
import static ru.i_novus.ms.rdm.impl.util.XlsxUtil.createNextRow;

/**
 * Created by znurgaliev on 24.07.2018.
 */
public class XlsxFileGenerator extends PerRowFileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(XlsxFileGenerator.class);

    private int pageSize = 500;

    private SXSSFWorkbook workbook;
    private XlsxCellStyleFactory styleFactory;

    private final Map<String, Integer> fieldColumns = new HashMap<>();

    public XlsxFileGenerator(Iterator<Row> rowIterator) {
        super(rowIterator);
    }

    public XlsxFileGenerator(Iterator<Row> rowIterator, Structure structure) {
        super(rowIterator, structure);
    }

    public XlsxFileGenerator(Iterator<Row> rowIterator, int pageSize) {
        super(rowIterator);

        this.pageSize = pageSize;
    }

    public XlsxFileGenerator(Iterator<Row> rowIterator, Structure structure, int pageSize) {
        super(rowIterator, structure);

        this.pageSize = pageSize;
    }

    @Override
    protected void startWrite() {
        logger.info("Start generate XLSX");

        workbook = new SXSSFWorkbook(500);
        styleFactory = new XlsxCellStyleFactory();

        final SXSSFSheet activeSheet = workbook.createSheet("Страница 1");
        createFirstRow(activeSheet);
    }

    @Override
    protected void write(Row row) {

        final org.apache.poi.ss.usermodel.Row sheetRow = createNextRow(getActiveSheet());
        row.getData().forEach((key, value) -> {
            final int columnIndex = getOrCreateColumnIndex(key);
            fillCell(sheetRow.createCell(columnIndex), value);
        });
    }

    @Override
    protected void endWrite() {

        final OutputStream ncos = new NoCloseOutputStreamWrapper(getOutputStream());
        try {
            autoSizeAllSheet();
            workbook.write(ncos);
            workbook.close();
            ncos.flush();
            fieldColumns.clear();
            styleFactory = null;

            logger.info("XLSX generate finished");

        } catch (IOException e) {
            logger.error("cannot generate XLSX", e);
            throw new RdmException("cannot generate XLSX");
        }
    }

    private SXSSFSheet getActiveSheet() {

        final int sheetIndex = workbook.getActiveSheetIndex();
        SXSSFSheet activeSheet = workbook.getSheetAt(sheetIndex);
        if (activeSheet.getPhysicalNumberOfRows() >= pageSize) {
            final int nextIndex = sheetIndex + 1;
            activeSheet = workbook.createSheet("Страница " + (nextIndex + 1));
            workbook.setActiveSheet(nextIndex);
            fieldColumns.clear();
            createFirstRow(activeSheet);
        }
        return activeSheet;
    }

    private SXSSFRow getActiveFirstRow() {

        final SXSSFSheet activeSheet = getActiveSheet();
        if (activeSheet.getPhysicalNumberOfRows() > 0)
            return activeSheet.getRow(activeSheet.getFirstRowNum());
        else
            return createFirstRow(activeSheet);
    }

    private SXSSFRow createFirstRow(SXSSFSheet sheet) {

        final SXSSFRow sheetRow = sheet.createRow(0);
        if (getStructure() != null) {
            getStructure().getAttributes().forEach(a -> getOrCreateColumnIndex(a.getCode()));
        }
        return sheetRow;
    }

    private int getOrCreateColumnIndex(String fieldName) {

        final Integer columnIndex = fieldColumns.get(fieldName);
        return columnIndex == null ? createColumn(fieldName) : columnIndex;
    }

    private int createColumn(String fieldName) {

        final int columnIndex = fieldColumns.size();

        final SXSSFCell currentCell = getActiveFirstRow().createCell(columnIndex);
        currentCell.setCellStyle(styleFactory.getFirstRowStyle());
        currentCell.setCellValue(fieldName);
        currentCell.getSheet().trackColumnForAutoSizing(columnIndex);

        fieldColumns.put(fieldName, columnIndex);

        return columnIndex;
    }

    private void fillCell(Cell cell, Object value) {

        if (value instanceof LocalDate) {
            final Date date = Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant());
            cell.setCellStyle(styleFactory.getDateStyle());
            cell.setCellValue(date);

        } else if (value instanceof Boolean) {
            cell.setCellStyle(styleFactory.getDefaultStyle());
            cell.setCellValue((Boolean) value);

        } else if (value instanceof Number) {
            cell.setCellStyle(styleFactory.getDefaultStyle());
            cell.setCellValue(((Number) value).doubleValue());

        } else if (value instanceof Reference) {
            cell.setCellStyle(styleFactory.getDefaultStyle());
            cell.setCellValue(((Reference) value).getValue());

        } else {
            cell.setCellStyle(styleFactory.getDefaultStyle());
            cell.setCellValue(Optional.ofNullable(value).orElse("").toString());
        }
    }

    private void autoSizeAllSheet() {
        for (int i = 0; i <= workbook.getActiveSheetIndex(); i++) {
            final SXSSFSheet sheet = workbook.getSheetAt(i);
            sheet.getTrackedColumnsForAutoSizing().forEach(sheet::autoSizeColumn);
        }
    }

    @Override
    public void close() throws IOException {
        if (workbook != null)
            workbook.close();
    }

    private static class NoCloseOutputStreamWrapper extends BufferedOutputStream {

        public NoCloseOutputStreamWrapper(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            //disable close for outputStream
        }
    }

    private class XlsxCellStyleFactory {

        private CellStyle firstRowStyle;
        private CellStyle defaultStyle;
        private CellStyle dateStyle;
        private Font defaultFont;

        private CellStyle getFirstRowStyle() {

            if (firstRowStyle == null) {
                final Font font = workbook.createFont();
                font.setFontHeightInPoints((short) 12);
                font.setFontName("Times New Roman");
                font.setBold(true);

                firstRowStyle = workbook.createCellStyle();
                firstRowStyle.setFont(font);
            }
            return firstRowStyle;
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
