package ru.inovus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.api.model.Result;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.impl.entity.AttributeValidationEntity;
import ru.inovus.ms.rdm.impl.validation.*;

import java.util.*;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

public class RowsValidatorImpl implements RowsValidator {

    private static final String ERROR_COUNT_EXCEEDED = "validation.error.count.exceeded";

    private Integer errorCountLimit = 100;
    private int size = 100;
    private List<Row> buffer = new ArrayList<>();

    private Result result = new Result(0, 0, null);

    private VersionService versionService;

    private SearchDataService searchDataService;

    private Structure structure;

    private String storageCode;

    private boolean skipReferenceValidation;

    private PkUniqueRowAppendValidation pkUniqueRowAppendValidation;

    private AttributeCustomValidation attributeCustomValidation;

    private boolean structureVerified;

    // NB: Add `RowsValidatorCriteria` to allow exclusion of some standard validations.
    public RowsValidatorImpl(VersionService versionService,
                             SearchDataService searchDataService,
                             Structure structure,
                             String storageCode,
                             int errorCountLimit,
                             boolean skipReferenceValidation,
                             List<AttributeValidationEntity> attributeValidations) {
        this.versionService = versionService;
        this.searchDataService = searchDataService;

        this.structure = structure;
        this.storageCode = storageCode;

        if (errorCountLimit > 0)
            this.errorCountLimit = errorCountLimit;

        this.skipReferenceValidation = skipReferenceValidation;
        this.pkUniqueRowAppendValidation = new PkUniqueRowAppendValidation(structure);
        this.attributeCustomValidation = new AttributeCustomValidation(attributeValidations, structure, searchDataService, storageCode);
    }

    @SuppressWarnings("squid:S00107")
    public RowsValidatorImpl(int size,
                             VersionService versionService,
                             SearchDataService searchDataService,
                             Structure structure,
                             String storageCode,
                             int errorCountLimit,
                             boolean skipReferenceValidation,
                             List<AttributeValidationEntity> attributeValidations) {
        this(versionService, searchDataService, structure, storageCode,
                errorCountLimit, skipReferenceValidation, attributeValidations);
        this.size = size;
    }

    @Override
    public Result append(Row row) {
        if (!structureVerified) {
            validateRowStructure(row);
            structureVerified = true;
        }
        if (row.getData().values().stream().filter(Objects::nonNull).anyMatch(v -> !"".equals(v))) {
            buffer.add(row);

            if (buffer.size() == size) {
                validate();
                buffer.clear();
            }
        }
        return this.result;
    }

    private void validateRowStructure(Row row) {

        List<Structure.Attribute> attributes = structure.getAttributes();
        if (attributes == null)
            return;

        boolean isNotKeyMatched = row.getData().keySet().stream()
                .anyMatch(key -> attributes.stream()
                        .noneMatch(attribute -> attribute.getCode().equals(key)));
        if (isNotKeyMatched)
            throw new UserException("structure.does-not-match");
    }

    @Override
    public Result process() {
        validate();
        if (!isEmpty(result.getErrors()))
            throw new UserException(result.getErrors());
        return result;
    }

    private void validate() {
        if (buffer.isEmpty()) {
            return;
        }

        DBPrimaryKeyValidation dbPrimaryKeyValidation = new DBPrimaryKeyValidation(searchDataService, structure, buffer, storageCode);

        buffer.forEach(row -> {
            List<Message> errors = new ArrayList<>();
            Set<String> errorAttributes = new HashSet<>();
            List<ErrorAttributeHolderValidation> validations = Arrays.asList(
                    new PkRequiredValidation(row, structure),
                    new TypeValidation(row.getData(), structure),
                    skipReferenceValidation ? null : new ReferenceValueValidation(versionService, row, structure),
                    dbPrimaryKeyValidation,
                    pkUniqueRowAppendValidation,
                    attributeCustomValidation
            );
            dbPrimaryKeyValidation.appendRow(row);
            pkUniqueRowAppendValidation.appendRow(row);
            attributeCustomValidation.appendRow(row);

            validations.stream()
                    .filter(Objects::nonNull)
                    .forEach(validation -> {
                        validation.setErrorAttributes(errorAttributes);
                        errors.addAll(validation.validate());
                        errorAttributes.addAll(validation.getErrorAttributes());
                    });

            if (isEmpty(errors)) {
                addResult(new Result(1, 1, null));
            } else {
                addResult(new Result(0, 1, errors));
            }
        });
    }

    private void addResult(Result result) {
        this.result = this.result == null ? result : this.result.addResult(result);
        if (this.result != null && this.result.getErrors().size() > errorCountLimit) {
            this.result.addResult(new Result(0, 0,
                    Collections.singletonList(new Message(ERROR_COUNT_EXCEEDED, errorCountLimit))));
            throw new UserException(this.result.getErrors());
        }
    }
}
