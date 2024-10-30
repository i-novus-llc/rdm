package ru.i_novus.ms.rdm.impl.file.process;

import com.monitorjbl.xlsx.StreamingReader;
import net.n2oapp.platform.i18n.UserException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.api.util.row.RowMapper;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static ru.i_novus.ms.rdm.api.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.util.XlsxUtil.XLSX_STYLE_DATE_FORMATTER;
import static ru.i_novus.ms.rdm.impl.util.XlsxUtil.getCellValue;
import static ru.i_novus.ms.rdm.impl.util.XlsxUtil.getStringCellValue;

public class XlsxPerRowProcessor extends FilePerRowProcessor {

    private static final Logger logger = LoggerFactory.getLogger(XlsxPerRowProcessor.class);

    private Workbook workbook;
    private Map<Integer, String> indexToNameMap;

    private Iterator<Sheet> sheetIterator;
    private Iterator<org.apache.poi.ss.usermodel.Row> rowIterator;

    public XlsxPerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor) {
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

            indexToNameMap = new HashMap<>();

            sheetIterator = workbook.sheetIterator();
            if (sheetIterator != null && sheetIterator.hasNext()) {
                rowIterator = sheetIterator.next().rowIterator();
            }

            if (rowIterator != null && rowIterator.hasNext()) {
                processFirstRow(rowIterator.next());
            }

        } catch (Exception e) {
            logger.error("cannot read xlsx", e);
            throw new UserException("cannot read xlsx");
        }
    }

    private void processFirstRow(org.apache.poi.ss.usermodel.Row row) {

        if (row == null) return;

        for (Cell cell : row) {
            final String value = getStringCellValue(cell);
            if (!isEmpty(value)) {
                indexToNameMap.put(cell.getColumnIndex(), value);
            }
        }
    }

    @Override
    public boolean hasNext() {

        if (rowIterator.hasNext())
            return true;

        if (!sheetIterator.hasNext())
            return false;

        rowIterator = sheetIterator.next().rowIterator();
        if (rowIterator.hasNext()) {
            processFirstRow(rowIterator.next());
        }

        return hasNext();
    }

    @Override
    public ru.i_novus.ms.rdm.api.model.refdata.Row next() {

        if (hasNext()) {
            return parseFromXlsx(rowIterator.next());
        }
        return null;
    }

    private ru.i_novus.ms.rdm.api.model.refdata.Row parseFromXlsx(org.apache.poi.ss.usermodel.Row row) {

        final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        indexToNameMap.values().forEach(name -> data.put(name, null));

        for (Cell cell : row) {
            final String name = indexToNameMap.get(cell.getColumnIndex());
            if (name != null) {
                data.put(name, getCellValue(cell, XLSX_STYLE_DATE_FORMATTER));
            }
        }
        return new ru.i_novus.ms.rdm.api.model.refdata.Row(data);
    }

    @Override
    public void close() throws IOException {

        if (workbook != null) {
            workbook.close();
        }
    }
}
