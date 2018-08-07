package ru.inovus.ms.rdm.file;

import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.util.ConverterUtil.field;

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
        validateReferences(row);
        return this.result;
    }

    @Override
    public Result process() {
        return this.result;
    }

    private void validateReferences(Row row) {
        List<Structure.Reference> references = structure.getReferences();
        List<Structure.Reference> invalidReferences = new ArrayList<>();
        if (!isEmpty(references)) {
            invalidReferences = references.stream()
                    .filter(reference -> {
                        String referenceValue = ((Reference) row.getData().get((reference).getAttribute())).getValue();
                        return !isReferenceValid(reference, referenceValue);
                    }).collect(Collectors.toList());
        }
        if (isEmpty(invalidReferences)) {
            this.result = this.result.addResult(new Result(1, 1, null));
        } else {
            String message = invalidReferences.stream()
                    .map(invalidReference -> invalidReference.getAttribute() + ": " + ((Reference) row.getData().get((invalidReference).getAttribute())).getValue())
                    .collect(Collectors.joining(", "));
            this.result = this.result.addResult(new Result(0, 1, Collections.singletonList(message)));
        }
    }

    public boolean isReferenceValid(Structure.Reference reference, String referenceValue) {
        Integer versionId = reference.getReferenceVersion();
        Structure referenceStructure = versionService.getStructure(versionId);
        Field fieldFilter = createFieldFilter(referenceStructure, reference);
        Object referenceValueCasted = castReferenceValue(fieldFilter, referenceValue);
        AttributeFilter attributeFilter = new AttributeFilter(reference.getReferenceAttribute(), referenceValueCasted,
                referenceStructure.getAttribute(reference.getReferenceAttribute()).getType(), SearchTypeEnum.EXACT);
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(Collections.singletonList(attributeFilter), null);
        Page<RowValue> pagedData = versionService.search(versionId, searchDataCriteria);
        return (pagedData != null && !isEmpty(pagedData.getContent()));
    }

    private Field createFieldFilter(Structure structure, Structure.Reference reference) {
        Structure.Attribute referenceAttribute = structure.getAttribute(reference.getReferenceAttribute());
        return field(referenceAttribute);
    }

    private Object castReferenceValue(Field field, String value) {
        switch (field.getType()) {
            case "boolean":
                return Boolean.valueOf(value);
            case "date":
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(value, formatter);
            case "numeric":
                return Float.parseFloat(value);
            case "bigint":
                return Integer.parseInt(value);
            case "varchar":
                return value;
            case "ltree":
                return value;
            default:
                throw new RdmException("invalid field type");
        }
    }

}
