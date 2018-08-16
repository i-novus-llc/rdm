package ru.inovus.ms.rdm.file;

import com.monitorjbl.xlsx.StreamingReader;
import net.n2oapp.platform.i18n.UserException;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class XlsPerRowProcessor extends FilePerRowProcessor {

    private static final Logger logger = LoggerFactory.getLogger(XlsPerRowProcessor.class);

    private Map<Integer, String> numberToNameParam = new HashMap<>();

    Workbook workbook;
    Iterator<Sheet> sheetIterator;
    Iterator<org.apache.poi.ss.usermodel.Row> rowIterator;


    public XlsPerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor) {
        super(rowMapper, rowsProcessor);
    }


    @Override
    protected void setFile(InputStream inputStream) {
        try {
            workbook = StreamingReader.builder()
                    .rowCacheSize(100)
                    .bufferSize(4096)
                    .open(inputStream);
            sheetIterator = workbook.sheetIterator();
            if (sheetIterator != null && sheetIterator.hasNext())
                rowIterator = sheetIterator.next().rowIterator();
            if (rowIterator != null && rowIterator.hasNext())
                processFirstRow(rowIterator.next());
        } catch (Exception e){
            logger.error("cannot read xlsx", e);
            throw new UserException("cannot read xlsx");
        }

    }

    private void processFirstRow(org.apache.poi.ss.usermodel.Row row) {
        if (row == null) return;
        for (Cell cell : row) {
            if (cell.getStringCellValue() != null && !"".equals(cell.getStringCellValue().trim()))
            numberToNameParam.put(cell.getColumnIndex(), cell.getStringCellValue());
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
    public Row next() {
        if (hasNext()) {
            return parseFromXlsx(rowIterator.next());
        }
        return null;
    }

    private Row parseFromXlsx(org.apache.poi.ss.usermodel.Row row) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        DataFormatter formatter = new DataFormatter();

        for (Cell cell : row) {
            String nameParam = numberToNameParam.get(cell.getColumnIndex());
            if (nameParam != null) {
                if (cell.getCellTypeEnum().equals(CellType.NUMERIC) && DateUtil.isCellDateFormatted(cell)) {
                    params.put(nameParam, new ExcelStyleDateFormatter("dd.MM.yyyy").format(cell.getDateCellValue()));
                } else params.put(nameParam, Optional.of(formatter.formatCellValue(cell).trim())
                        .filter(val -> !"".equals(val))
                        .orElse(null));
            }
        }
        return new Row(params);
    }

    @Override
    public void close() throws IOException {
        if (workbook != null)
            workbook.close();
    }
}
