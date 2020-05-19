package ru.inovus.ms.rdm.impl.file.process;

import com.monitorjbl.xlsx.StreamingReader;
import net.n2oapp.platform.i18n.UserException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.impl.util.mappers.RowMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.springframework.util.StringUtils.isEmpty;

public class XlsPerRowProcessor extends FilePerRowProcessor {

    private static final Logger logger = LoggerFactory.getLogger(XlsPerRowProcessor.class);

    private final ExcelStyleDateFormatter excelStyleDateFormatter = new ExcelStyleDateFormatter("dd.MM.yyyy");

    private Map<Integer, String> columnNames = new HashMap<>();

    private Workbook workbook;
    private Iterator<Sheet> sheetIterator;
    private Iterator<org.apache.poi.ss.usermodel.Row> rowIterator;

    private DataFormatter dataFormatter;

    public XlsPerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor) {
        super(rowMapper, rowsProcessor);
    }

    @Override
    protected void setFile(InputStream inputStream) {
        try {
            workbook = StreamingReader.builder()
                    .rowCacheSize(2000)
                    .bufferSize(8096)
                    .sstCacheSize(3000)
                    .open(inputStream);
            sheetIterator = workbook.sheetIterator();
            if (sheetIterator != null && sheetIterator.hasNext())
                rowIterator = sheetIterator.next().rowIterator();
            if (rowIterator != null && rowIterator.hasNext())
                processFirstRow(rowIterator.next());

            dataFormatter = new DataFormatter();

        } catch (Exception e) {
            logger.error("cannot read xlsx", e);
            throw new UserException("cannot read xlsx");
        }

    }

    private void processFirstRow(org.apache.poi.ss.usermodel.Row row) {
        if (row == null) return;
        for (Cell cell : row) {
            String value = cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : null;
            if (!isEmpty(value))
                columnNames.put(cell.getColumnIndex(), value);
        }
    }

    @Override
    public boolean hasNext() {
        if (rowIterator.hasNext()) {
            return true;
        } else if (sheetIterator.hasNext()) {
            rowIterator = sheetIterator.next().rowIterator();
            if (rowIterator.hasNext())
                processFirstRow(rowIterator.next());
        } else return false;
        return hasNext();
    }

    @Override
    public ru.inovus.ms.rdm.api.model.refdata.Row next() {
        if (hasNext()) {
            return parseFromXlsx(rowIterator.next());
        }
        return null;
    }

    private ru.inovus.ms.rdm.api.model.refdata.Row parseFromXlsx(org.apache.poi.ss.usermodel.Row row) {

        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        columnNames.values().forEach(name -> data.put(name, null));

        for (Cell cell : row) {
            String name = columnNames.get(cell.getColumnIndex());
            if (name != null) {
                data.put(name, getCellValue(cell, dataFormatter));
            }
        }
        return new ru.inovus.ms.rdm.api.model.refdata.Row(data);
    }

    private String getCellValue(Cell cell, DataFormatter dataFormatter) {

        if (cell.getCellTypeEnum().equals(CellType.NUMERIC)
                && DateUtil.isCellDateFormatted(cell)) {
            return excelStyleDateFormatter.format(cell.getDateCellValue());
        }

        if (cell.getCellTypeEnum().equals(CellType.FORMULA))
            return getCachedCellValue(cell);

        String value = dataFormatter.formatCellValue(cell).trim();
        return !isEmpty(value) ? value : null;
    }

    private String getCachedCellValue(Cell cell) {

        switch (cell.getCachedFormulaResultTypeEnum()) {
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return excelStyleDateFormatter.format(cell.getDateCellValue());
                }

                return getNumericCellValue(cell);

            case STRING:
                return cell.getRichStringCellValue().getString();

            case BOOLEAN:
                return cell.getBooleanCellValue() ? "TRUE" : "FALSE";

            default:
                return cell.getCellFormula();
        }
    }

    private String getNumericCellValue(Cell cell) {

        // dataFormatter.getFormattedNumberString недоступен.
        String value = toNumeric(cell.getStringCellValue());
        return !isEmpty(value) ? value : String.valueOf(cell.getNumericCellValue());
    }

    private String toNumeric(String value) {

        if (value == null) return null;

        return value.replace(',', '.')
                .replaceAll("\"", "");
    }

    @Override
    public void close() throws IOException {
        if (workbook != null)
            workbook.close();
    }
}
