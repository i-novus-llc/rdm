package ru.i_novus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;
import ru.i_novus.platform.datastorage.temporal.exception.NotUniqueException;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.rowValue;

@SuppressWarnings("java:S3740")
public class BufferedRowsPersister implements RowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BufferedRowsPersister.class);

    private static final String ROWS_ERROR_EXCEPTION_CODE = "rows.error";
    private static final String ROW_NOT_UNIQUE_EXCEPTION_CODE = "row.not.unique";

    private final DraftDataService draftDataService;

    private final String storageCode;

    private final List<Row> buffer = new ArrayList<>();

    private Structure structure;

    private int size = 500;

    private Result result = new Result(0, 0, null);

    public BufferedRowsPersister(DraftDataService draftDataService, String storageCode, Structure structure) {

        this.draftDataService = draftDataService;
        this.storageCode = storageCode;
        this.structure = structure;
    }

    public BufferedRowsPersister(DraftDataService draftDataService, String storageCode, Structure structure, int size) {

        this(draftDataService, storageCode, structure);

        this.size = size;
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

        if (buffer.isEmpty())
            return;

        List<RowValue> rowValues = buffer.stream()
                .filter(row -> row.getData().values().stream().anyMatch(Objects::nonNull))
                .map(row -> rowValue(row, structure)).collect(toList());
        try {
            draftDataService.addRows(storageCode, rowValues);
            setSuccessResult(rowValues);

        } catch (NotUniqueException e) {
            setErrorResult(ROW_NOT_UNIQUE_EXCEPTION_CODE, e);

        } catch (Exception e) {
            setErrorResult(ROWS_ERROR_EXCEPTION_CODE, e);
        }
    }

    private void setSuccessResult(List<RowValue> rowValues) {

        this.result = this.result.addResult(
                new Result(rowValues.size(), buffer.size(), null)
        );
    }

    private void setErrorResult(String errorCode, Exception e) {

        this.result = this.result.addResult(
                new Result(0, buffer.size(), singletonList(new Message(errorCode, e.getMessage())))
        );

        logger.error("cannot add rows", e);
    }

}
