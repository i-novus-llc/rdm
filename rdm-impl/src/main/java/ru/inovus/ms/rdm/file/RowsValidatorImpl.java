package ru.inovus.ms.rdm.file;

import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.VersionService;

import java.util.Collections;
import java.util.List;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.util.ConverterUtil.attributeToField;

public class RowsValidatorImpl implements RowsValidator {

    private Result result = new Result(0, 0, null);

    private Structure structure;

    private FieldFactory fieldFactory;

    private VersionService versionService;

    public RowsValidatorImpl(VersionService versionService, FieldFactory fieldFactory, Structure structure) {
        this.versionService = versionService;
        this.fieldFactory = fieldFactory;
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
        boolean isRowValid = true;
        if (!isEmpty(references)) {
            isRowValid = references.stream().allMatch(reference -> {
                Object referenceValue = row.getData().get((reference).getAttribute());
                return validateReference(reference, referenceValue);
            });
        }
        if (isRowValid) {
            this.result = this.result.addResult(new Result(1, 1, null));
        } else {
            this.result = this.result.addResult(new Result(0, 1, Collections.singletonList("Reference in row is not valid")));
        }
    }

    private boolean validateReference(Structure.Reference reference, Object referenceValue) {
        Integer versionId = reference.getReferenceVersion();
        Structure referenceStructure = versionService.getStructure(versionId);
        Field fieldFilter = createFieldFilter(referenceStructure, reference);
        FieldSearchCriteria searchCriteria = new FieldSearchCriteria(fieldFilter, SearchTypeEnum.EXACT, Collections.singletonList(referenceValue));
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria(Collections.singletonList(searchCriteria), null);
        Page<RowValue> pagedData = versionService.search(versionId, searchDataCriteria);
        return (pagedData != null && !isEmpty(pagedData.getContent()));
    }

    private Field createFieldFilter(Structure structure, Structure.Reference reference) {
        Structure.Attribute referenceAttribute = structure.getAttribute(reference.getReferenceAttribute());
        return attributeToField(referenceAttribute, fieldFactory);
    }
}
