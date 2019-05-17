package ru.inovus.ms.rdm.file.process;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.AttributeValidationEntity;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.validation.*;

import java.util.*;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

public class RowsValidatorImpl implements RowsValidator {

    public static final String ERROR_COUNT_EXCEEDED = "validation.error.count.exceeded";

    private Integer errorCountLimit = 100;

    private Result result = new Result(0, 0, null);

    private Structure structure;

    private VersionService versionService;

    private SearchDataService searchDataService;

    private String storageCode;

    private PkUniqueRowAppendValidation pkUniqueRowAppendValidation;

    private AttributeCustomValidation attributeCustomValidation;


    public RowsValidatorImpl(VersionService versionService,
                             SearchDataService searchDataService,
                             Structure structure,
                             String storageCode,
                             int errorCountLimit,
                             List<AttributeValidationEntity> attributeValidations) {
        this.versionService = versionService;
        this.structure = structure;
        this.searchDataService = searchDataService;
        this.storageCode = storageCode;
        this.pkUniqueRowAppendValidation = new PkUniqueRowAppendValidation(structure);
        this.attributeCustomValidation = new AttributeCustomValidation(attributeValidations, structure, searchDataService, storageCode);
        if (errorCountLimit > 0) this.errorCountLimit = errorCountLimit;
    }

    @Override
    public Result append(Row row) {
        List<Message> errors = new ArrayList<>();
        Set<String> errorAttributes = new HashSet<>();
        if (row.getData().values().stream().filter(Objects::nonNull).anyMatch(v -> !"".equals(v))) {
            List<ErrorAttributeHolderValidation> validations = Arrays.asList(
                    new PkRequiredValidation(row, structure),
                    new TypeValidation(row.getData(), structure),
                    new ReferenceValueValidation(versionService, row, structure),
                    new DBPrimaryKeyValidation(searchDataService, structure, row, storageCode),
                    pkUniqueRowAppendValidation,
                    attributeCustomValidation
            );
            pkUniqueRowAppendValidation.appendRow(row);
            attributeCustomValidation.appendRow(row);

            for (ErrorAttributeHolderValidation validation : validations) {
                validation.setErrorAttributes(errorAttributes);
                errors.addAll(validation.validate());
                errorAttributes.addAll(validation.getErrorAttributes());
            }
        }

        if (isEmpty(errors)) {
            addResult(new Result(1, 1, null));
        } else {
            addResult(new Result(0, 1, errors));
        }
        return this.result;
    }


    @Override
    public Result process() {
        if (!CollectionUtils.isEmpty(result.getErrors()))
            throw new UserException(result.getErrors());
        return result;
    }

    private void addResult(Result result) {
        this.result = this.result == null ? result : this.result.addResult(result);
        if (this.result != null && this.result.getErrors().size() > errorCountLimit) {
            this.result.addResult(new Result(0, 0, Collections.singletonList(new Message(ERROR_COUNT_EXCEEDED, errorCountLimit))));
            throw new UserException(this.result.getErrors());
        }
    }
}
