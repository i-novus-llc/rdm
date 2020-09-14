package ru.i_novus.ms.rdm.n2o.l10n.service;

import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.api.service.RefBookDataService;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.BooleanFieldValue;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Primary
@Service
@SuppressWarnings("java:S3740")
public class L10nRefBookDataServiceImpl implements RefBookDataService {

    public static final String SYS_LOCALIZED = "SYS_LOCALIZED"; // from l10n-vds L10nConstants

    private static final String ATTRIBUTE_LOCALIZED_NAME = "attribute.localized.name";
    private static final String ATTRIBUTE_LOCALIZED_DESCRIPTION = "attribute.localized.description";
    private static final String ATTRIBUTE_LOCALIZED_NONE = "attribute.localized.none";
    private static final String ATTRIBUTE_LOCALIZED_MADE = "attribute.localized.made";

    private VersionRestService versionService;

    private Messages messages;

    @Autowired
    public L10nRefBookDataServiceImpl(VersionRestService versionService,
                                      Messages messages) {
        this.versionService = versionService;

        this.messages = messages;
    }

    @Override
    public Structure getDataStructure(Integer versionId, DataCriteria criteria) {

        Structure structure = versionService.getStructure(versionId);
        if (criteria.getLocaleCode() == null)
            return structure;

        return toL10nDataStructure(structure);
    }

    private Structure toL10nDataStructure(Structure structure) {

        Structure result = new Structure(structure);

        Structure.Attribute localized = Structure.Attribute.build(SYS_LOCALIZED,
                messages.getMessage(ATTRIBUTE_LOCALIZED_NAME),
                FieldType.STRING,
                messages.getMessage(ATTRIBUTE_LOCALIZED_DESCRIPTION)
        );
        result.add(localized, null);

        return result;
    }

    @Override
    public List<RefBookRowValue> getDataContent(List<RefBookRowValue> searchContent, DataCriteria criteria) {

        if (criteria.getLocaleCode() == null || isEmpty(searchContent))
            return searchContent;

        return toL10nDataContent(searchContent);
    }

    private List<RefBookRowValue> toL10nDataContent(List<RefBookRowValue> searchContent) {

        return searchContent.stream().map(this::toL10nDataRowValue).collect(toList());
    }

    private RefBookRowValue toL10nDataRowValue(RefBookRowValue rowValue) {

        List<FieldValue> fieldValues = rowValue.getFieldValues();
        if (!isEmpty(fieldValues)) {
            fieldValues = fieldValues.stream().map(this::toL10nDataFieldValue).collect(toList());
            rowValue.setFieldValues(fieldValues);
        }
        return rowValue;
    }

    @SuppressWarnings("unchecked")
    private FieldValue toL10nDataFieldValue(FieldValue fieldValue) {

        if (SYS_LOCALIZED.equals(fieldValue.getField()) && fieldValue instanceof BooleanFieldValue) {
            fieldValue.setValue(localizedToString(((BooleanFieldValue) fieldValue).getValue()));
        }
        return fieldValue;
    }

    private String localizedToString(Boolean value) {

        String result = Boolean.TRUE.equals(value) ? ATTRIBUTE_LOCALIZED_MADE : ATTRIBUTE_LOCALIZED_NONE;
        return messages.getMessage(result);
    }
}
