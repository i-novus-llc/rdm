package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.validation.ReferenceValueValidation;
import ru.inovus.ms.rdm.validation.TypeValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

public class RowsValidatorImpl implements RowsValidator {

    private Result result = new Result(0, 0, null);

    private Structure structure;

    private VersionService versionService;

    public RowsValidatorImpl(VersionService versionService, Structure structure) {
        this.versionService = versionService;
        this.structure = structure;
    }

    @Override
    public Result append(Row row) {
        List<Message> errors = new ArrayList<>();
        TypeValidation typeValidation = new TypeValidation(row.getData(), structure);
        //noinspection unchecked
        errors.addAll(executeValidations(row, Collections.<String>emptyList(), (unusedRowArg, unusedExcludeAttributes) -> typeValidation.validate()));
        errors.addAll(executeValidations(row, typeValidation.getErrorAttributes(), this::validateReferences));
        if(isEmpty(errors)){
            this.result = this.result.addResult(new Result(1, 1, null));
        } else {
            this.result = this.result.addResult(new Result(0, 1, errors));
        }
        return this.result;
    }


    @Override
    public Result process() {
        return this.result;
    }

    private List<Message> executeValidations(Row row, List<String> excludeAttributes, BiFunction<Row, List<String>, List<Message>>... validationFunction ) {
        List<Message> messages = new ArrayList<>();
        for (BiFunction<Row, List<String>, List<Message>> validate : validationFunction) {
            messages.addAll(validate.apply(row, excludeAttributes));
        }

        return messages;
    }
    private List<Message> validateReferences(Row row, List<String> excludeAttributes) {
        Map<Structure.Reference, String> referencesWithValueMap = structure.getReferences().stream()
                .filter(reference -> row.getData().get(reference.getAttribute()) != null)
                .collect(Collectors.toMap(reference -> reference, reference -> getReferenceStringValue(row, reference)));
        return new ReferenceValueValidation(versionService, referencesWithValueMap, structure, excludeAttributes).validate();
    }

    private String getReferenceStringValue(Row row, Structure.Reference reference) {
        return ((Reference) row.getData().get(reference.getAttribute())).getValue();
    }
}
