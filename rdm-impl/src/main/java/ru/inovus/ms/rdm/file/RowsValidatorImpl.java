package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.Message;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.validation.TypeValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.util.ConverterUtil.castReferenceValue;
import static ru.inovus.ms.rdm.util.ConverterUtil.field;

public class RowsValidatorImpl implements RowsValidator {

    private Result result = new Result(0, 0, null);

    private Structure structure;

    private VersionService versionService;

    private List<Message> messages = new ArrayList<>();

    public RowsValidatorImpl(VersionService versionService, Structure structure) {
        this.versionService = versionService;
        this.structure = structure;
    }

    @Override
    public Result append(Row row) {
        Optional.ofNullable(validateReferences(row)).ifPresent(referenceMessages -> messages.addAll(referenceMessages));
        TypeValidation typeValidation = new TypeValidation(row.getData(), structure);
        Optional.ofNullable(typeValidation.validate()).ifPresent(typeMessages -> messages.addAll(typeMessages));
        if (!isEmpty(messages)) {
            this.result = this.result.addResult(new Result(0, 1, messages))
        } else {
            this.result = this.result.addResult(new Result(1, 1, null));
        }
        return this.result;
    }


    @Override
    public Result process() {
        return this.result;
    }

    private List<Message> validateReferences(Row row) {
        List<Structure.Reference> references = structure.getReferences();
        List<Structure.Reference> invalidReferences = new ArrayList<>();
        if (!isEmpty(references)) {
            invalidReferences = references.stream()
                    .filter(reference -> {
                        String referenceValue = ((Reference) row.getData().get((reference).getAttribute())).getValue();
                        return !isReferenceValid(reference, referenceValue);
                    }).collect(Collectors.toList());
        }
        if (!isEmpty(invalidReferences)) {
            return invalidReferences.stream()
                    .map(invalidReference -> new Message("validation.reference.err", invalidReference.getAttribute() + ": " + ((Reference) row.getData().get((invalidReference).getAttribute())).getValue()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean isReferenceValid(Structure.Reference reference, String referenceValue) {
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

}
