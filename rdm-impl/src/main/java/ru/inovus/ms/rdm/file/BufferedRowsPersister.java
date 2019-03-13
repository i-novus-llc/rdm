package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.util.ConverterUtil.rowValue;

public class BufferedRowsPersister implements RowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BufferedRowsPersister.class);

    public static final String ROWS_ERROR_EXCEPTION_CODE = "rows.error";
    public static final String ROW_NOT_UNIQUE_EXCEPTION_CODE = "row.not.unique";

    private int size = 100;

    private List<Row> buffer = new ArrayList<>();

    private DraftDataService draftDataService;

    private String storageCode;

    private Structure structure;

    private Result result = new Result(0, 0, null);

    public BufferedRowsPersister(DraftDataService draftDataService, String storageCode, Structure structure) {
        this.draftDataService = draftDataService;
        this.storageCode = storageCode;
        this.structure = structure;
    }

    public BufferedRowsPersister(int size, DraftDataService draftDataService, String storageCode, Structure structure) {
        this.size = size;
        this.draftDataService = draftDataService;
        this.storageCode = storageCode;
        this.structure = structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    @Override
    public Result append(Row row) {

        if (row.getData().values().stream().anyMatch(Objects::nonNull)) {
            buffer.add(row);

            if (buffer.size() == size) {
                save();
                buffer.clear();
            }
        }
        return this.result;
    }

    @Override
    public Result process() {
        save();
        if (!isEmpty(result.getErrors()))
            throw new UserException(result.getErrors());
        return result;
    }

    private void save() {
        if (buffer.isEmpty()) {
            return;
        }
        List<RowValue> rowValues = buffer.stream()
                .filter(row -> row.getData().values().stream().anyMatch(Objects::nonNull))
                .map(row -> rowValue(row, structure)).collect(toList());
        try {
            draftDataService.addRows(storageCode, rowValues);
            this.result = this.result.addResult(new Result(rowValues.size(), buffer.size(), null));
        } catch (NotUniqueException e) {
            setErrorResult(ROW_NOT_UNIQUE_EXCEPTION_CODE, e);
        } catch (Exception e) {
            setErrorResult(ROWS_ERROR_EXCEPTION_CODE, e);
        }
    }

    private void setErrorResult(String errorCode, Exception e) {
        this.result = this.result.addResult(new Result(0, buffer.size(), singletonList(new Message(errorCode, e.getMessage()))));
        logger.error("can not add rows", e);
    }

}
