package ru.i_novus.ms.rdm.n2o.l10n.service;

import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.api.service.RefBookDataService;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

@Primary
@Service
public class L10nRefBookDataServiceImpl implements RefBookDataService {

    public static final String SYS_LOCALIZED = "SYS_LOCALIZED"; // from l10n-vds L10nConstants
    private static final String ATTRIBUTE_LOCALIZED_NAME = "attribute.localized.name";
    private static final String ATTRIBUTE_LOCALIZED_DESCRIPTION = "attribute.localized.description";
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
                FieldType.BOOLEAN,
                messages.getMessage(ATTRIBUTE_LOCALIZED_DESCRIPTION)
        );
        result.add(localized, null);

        return result;
    }
}
