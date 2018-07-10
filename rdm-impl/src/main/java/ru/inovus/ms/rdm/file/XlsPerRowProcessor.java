package ru.inovus.ms.rdm.file;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class XlsPerRowProcessor extends FilePerRowProcessor {

    private static final Logger logger = LoggerFactory.getLogger(XlsPerRowProcessor.class);

    private Map<Integer, String> numberToNameParam = new HashMap<>();

    private Sheet sheet;

    private CellStyle dateCellStyle;

    private int index = 0;

    public XlsPerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor) {
        super(rowMapper, rowsProcessor);
    }

    public XlsPerRowProcessor(RowsProcessor rowsProcessor) {
        super(rowsProcessor);
    }

    @Override
    protected void setFile(InputStream inputStream) {
        try (Workbook wb = WorkbookFactory.create(inputStream)) {
            this.numberToNameParam = new HashMap<>();
            this.sheet = wb.getSheetAt(0);
            this.dateCellStyle = wb.createCellStyle();
            dateCellStyle.setDataFormat(wb.createDataFormat().getFormat("dd.MM.yyyy"));


            org.apache.poi.ss.usermodel.Row firstRow = sheet.getRow(0);
            for (int i = 0; i < firstRow.getPhysicalNumberOfCells(); i++) {
                String cellFieldName = new String(firstRow.getCell(i).getStringCellValue().getBytes(Charset.forName("UTF-8")));
                if (!"".equals(cellFieldName))
                    numberToNameParam.put(i, cellFieldName);
            }
        } catch (InvalidFormatException | IOException e) {
            logger.error("cannot parse xls", e);
            throw new IllegalArgumentException("invalid file");
        }
    }

    @Override
    public boolean hasNext() {
        int i = index + 1;
        org.apache.poi.ss.usermodel.Row row = null;
        while (row == null && i < sheet.getPhysicalNumberOfRows()) {
            row = sheet.getRow(index);
            i++;
        }
        return row != null;
    }

    @Override
    public Row next() {
        org.apache.poi.ss.usermodel.Row row = null;
        while (row == null && index < sheet.getPhysicalNumberOfRows()) {
            index++;
            row = sheet.getRow(index);
        }
        if (row != null) {
            Map<String, Object> params = new HashMap<>();
            for (int j = 0; j < numberToNameParam.size(); j++) {
                Cell cell = row.getCell(j);
                if (cell != null) {
                    DataFormatter formatter = new DataFormatter();
                    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        cell.setCellStyle(dateCellStyle);
                    }
                    params.put(numberToNameParam.get(j), Optional.of(formatter.formatCellValue(cell).trim())
                            .filter(val -> !val.equals(""))
                            .orElse(null));
                }
            }
            return new Row(params);
        } else {
            return null;
        }

    }
}
