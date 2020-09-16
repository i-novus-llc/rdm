package ru.i_novus.ms.rdm.impl.validation;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.io.Serializable;
import java.time.format.DateTimeParseException;
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

@SuppressWarnings("java:S3740")
public class ReferenceValidation implements RdmValidation {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceValidation.class);

    private static final String LAST_PUBLISHED_NOT_FOUND_EXCEPTION_CODE = "last.published.not.found";
    private static final String VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE = "version.has.not.structure";
    private static final String VERSION_PRIMARY_KEY_NOT_FOUND_EXCEPTION_CODE = "version.primary.key.not.found";
    private static final String VERSION_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "version.attribute.not.found";
    private static final String ATTRIBUTE_VALUE_INCONVERTIBLE_TO_NEW_TYPE_EXCEPTION_CODE = "attribute.value.inconvertible.to.new.type";

    private SearchDataService searchDataService;
    private RefBookVersionRepository versionRepository;

    private Structure.Reference reference;
    private Integer draftId;
    private Integer bufSize;

    public ReferenceValidation(SearchDataService searchDataService,
                               RefBookVersionRepository versionRepository,
                               Structure.Reference reference, Integer draftId) {
        this(searchDataService, versionRepository, reference, draftId, 100);
    }

    @SuppressWarnings("WeakerAccess")
    public ReferenceValidation(SearchDataService searchDataService,
                               RefBookVersionRepository versionRepository,
                               Structure.Reference reference, Integer draftId, Integer bufSize) {
        this.searchDataService = searchDataService;
        this.versionRepository = versionRepository;
        this.reference = reference;
        this.draftId = draftId;
        this.bufSize = bufSize;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> validate() {

        RefBookVersionEntity draftEntity = versionRepository.getOne(draftId);
        Structure.Attribute draftAttribute = draftEntity.getStructure().getAttribute(reference.getAttribute());
        Field draftField = ConverterUtil.field(draftAttribute);

        // Использовать VersionValidationImpl.validateReferenceCode
        RefBookVersionEntity referredEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(reference.getReferenceCode(), RefBookVersionStatus.PUBLISHED);
        if (Objects.isNull(referredEntity))
            return singletonList(new Message(LAST_PUBLISHED_NOT_FOUND_EXCEPTION_CODE, reference.getReferenceCode()));
        if (Objects.isNull(referredEntity.getStructure()))
            return singletonList(new Message(VERSION_HAS_NOT_STRUCTURE_EXCEPTION_CODE, referredEntity.getId()));

        Structure.Attribute referredAttribute;
        try {
            referredAttribute = reference.findReferenceAttribute(referredEntity.getStructure());

        } catch (RdmException e) {
            logger.info(VERSION_PRIMARY_KEY_NOT_FOUND_EXCEPTION_CODE, e);
            return singletonList(new Message(VERSION_PRIMARY_KEY_NOT_FOUND_EXCEPTION_CODE, referredEntity.getId()));
        }
        Field referredField = ConverterUtil.field(referredAttribute);

        // Поля из вычисляемого выражения, отсутствующие в версии, на которую ссылаемся.
        List<String> incorrectFields = StructureUtils.getAbsentPlaceholders(reference.getDisplayExpression(), referredEntity.getStructure());
        if (!isEmpty(incorrectFields)) {
            return incorrectFields.stream()
                    .map(field -> new Message(VERSION_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE, referredEntity.getId(), field))
                    .collect(toList());
        }

        StorageDataCriteria draftDataCriteria = new StorageDataCriteria(draftEntity.getStorageCode(), null, null,
                singletonList(draftField), emptySet(), null);
        draftDataCriteria.setPage(BaseDataCriteria.MIN_PAGE);
        draftDataCriteria.setSize(bufSize);

        // Значения, не приводимые к типу атрибута, на который ссылаемся,
        // либо значения, не найденные в версии, на которую ссылаемся.
        List<String> incorrectValues = new ArrayList<>();
        validateData(draftDataCriteria, incorrectValues, referredEntity, referredField);

        return incorrectValues.stream()
                .map(value -> new Message(ATTRIBUTE_VALUE_INCONVERTIBLE_TO_NEW_TYPE_EXCEPTION_CODE, draftAttribute.getName(), value))
                .collect(toList());
    }

    // NB: Странный рекурсивный проход по страницам.
    private void validateData(StorageDataCriteria draftDataCriteria, List<String> incorrectValues,
                              RefBookVersionEntity referredEntity, Field referredField) {
        CollectionPage<RowValue> draftRowValues = searchDataService.getPagedData(draftDataCriteria);
        // Значения, которые приведены к типу атрибута из ссылки
        List<Serializable> castedValues = new ArrayList<>();

        (draftRowValues.getCollection()).forEach(rowValue -> {
            String value = String.valueOf(rowValue.getFieldValue(reference.getAttribute()).getValue());
            Serializable castedValue;
            try {
                castedValue = ConverterUtil.castReferenceValue(referredField, value);
                castedValues.add(castedValue);

            } catch (NumberFormatException | DateTimeParseException | RdmException e) {
                incorrectValues.add(value);
                logger.error(String.format("Can not parse value %s", value), e);
            }
        });

        if (!isEmpty(castedValues)) {
            FieldSearchCriteria refFieldSearchCriteria = new FieldSearchCriteria(referredField, SearchTypeEnum.EXACT, castedValues);
            StorageDataCriteria referredDataCriteria = new StorageDataCriteria(referredEntity.getStorageCode(),
                    referredEntity.getFromDate(), referredEntity.getToDate(),
                    singletonList(referredField), singletonList(refFieldSearchCriteria), null);
            CollectionPage<RowValue> refRowValuePage = searchDataService.getPagedData(referredDataCriteria);
            Collection<RowValue> refRowValues = (refRowValuePage.getCollection() != null) ? refRowValuePage.getCollection() : emptyList();

            castedValues.forEach(castedValue -> {
                if (refRowValues.stream()
                        .noneMatch(rowValue ->
                                castedValue.equals(rowValue.getFieldValue(referredField.getName()).getValue())
                        ))
                    incorrectValues.add(String.valueOf(castedValue));
            });
        }

        int remainCount = draftRowValues.getCount() - (draftDataCriteria.getPage() - 1) * bufSize - draftDataCriteria.getSize();
        if (remainCount <= 0)
            return;

        draftDataCriteria.setPage(draftDataCriteria.getPage() + 1);
        draftDataCriteria.setSize((remainCount >= bufSize) ? bufSize : remainCount);
        validateData(draftDataCriteria, incorrectValues, referredEntity, referredField);
    }
}
