package ru.i_novus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.entity.AttributeValidationEntity;
import ru.i_novus.ms.rdm.impl.validation.*;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;

public class RowsValidatorImpl implements RowsValidator {

    private static final String ERROR_COUNT_EXCEEDED = "validation.error.count.exceeded";

    private Integer errorCountLimit = 100;
    private int size = 100;
    private final List<Row> buffer = new ArrayList<>();

    private Result result = new Result(0, 0, null);

    private final VersionService versionService;

    private final SearchDataService searchDataService;

    private final Structure structure;

    private final String storageCode;

    private final boolean skipReferenceValidation;

    private final PkUniqueRowAppendValidation pkUniqueRowAppendValidation;

    private final AttributeCustomValidation attributeCustomValidation;

    private boolean structureVerified;
    private final Set<String> structFields;

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
        this.structFields = new HashSet<>(structure.getAttributeCodes());
        this.storageCode = storageCode;

        if (errorCountLimit > 0) {
            this.errorCountLimit = errorCountLimit;
        }
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

        if (structure.isEmpty())
            return;

        if (!structFields.containsAll(row.getData().keySet())) {
            throw new UserException("loaded.structure.not.match");
        }
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

        DBPrimaryKeyValidation dbPrimaryKeyValidation = new DBPrimaryKeyValidation(searchDataService, storageCode, structure, buffer);
        ReferenceValueValidation referenceValueValidation = skipReferenceValidation
                ? null
                : new ReferenceValueValidation(versionService, structure, buffer);

        buffer.forEach(row -> {
            List<Message> errors = new ArrayList<>();
            Set<String> errorAttributes = new HashSet<>();
            List<ErrorAttributeHolderValidation> validations = Arrays.asList(
                    new PkRequiredValidation(row, structure),
                    new TypeValidation(row.getData(), structure),
                    referenceValueValidation,
                    dbPrimaryKeyValidation,
                    pkUniqueRowAppendValidation,
                    attributeCustomValidation
            );

            validations.stream()
                    .filter(validation -> validation instanceof AppendRowValidation)
                    .forEach(validation -> ((AppendRowValidation) validation).appendRow(row));

            validations.stream()
                    .filter(Objects::nonNull)
                    .forEach(validation -> {
                        validation.setErrorAttributes(errorAttributes);
                        errors.addAll(validation.validate());
                        errorAttributes.addAll(validation.getErrorAttributes());
                    });

            addResult(errors);
        });
    }

    private void addResult(List<Message> errors) {

        boolean hasErrors = isEmpty(errors);
        addResult(new Result(hasErrors ? 1 : 0, 1, hasErrors ? null : errors));
    }

    private void addResult(Result result) {

        this.result = (this.result == null) ? result : this.result.addResult(result);

        if (this.result != null && this.result.getErrors().size() > errorCountLimit) {
            Message error = new Message(ERROR_COUNT_EXCEEDED, errorCountLimit);
            this.result.addResult(new Result(0, 0, singletonList(error)));
            throw new UserException(this.result.getErrors());
        }
    }
}
