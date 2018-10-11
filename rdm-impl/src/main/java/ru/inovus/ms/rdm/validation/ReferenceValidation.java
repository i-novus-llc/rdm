package ru.inovus.ms.rdm.validation;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.time.format.DateTimeParseException;
import java.util.*;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.apache.cxf.common.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.util.ConverterUtil.date;
import static ru.inovus.ms.rdm.util.ConverterUtil.field;

public class ReferenceValidation implements RdmValidation {

    private SearchDataService searchDataService;
    private RefBookVersionRepository versionRepository;
    private Structure.Reference reference;
    private Integer draftId;
    private Integer bufSize;

    private static final Logger logger = LoggerFactory.getLogger(ReferenceValidation.class);
    private static final String INCONVERTIBLE_DATA_TYPES_EXCEPTION_CODE = "inconvertible.new.type";

    public ReferenceValidation(SearchDataService searchDataService, RefBookVersionRepository versionRepository, Structure.Reference reference, Integer draftId) {
        this(searchDataService, versionRepository, reference, draftId, 100);
    }

    public ReferenceValidation(SearchDataService searchDataService, RefBookVersionRepository versionRepository, Structure.Reference reference, Integer draftId, Integer bufSize) {
        this.searchDataService = searchDataService;
        this.versionRepository = versionRepository;
        this.reference = reference;
        this.draftId = draftId;
        this.bufSize = bufSize;
    }

    @Override
    public List<Message> validate() {
        RefBookVersionEntity draftVersion = versionRepository.getOne(draftId);
        RefBookVersionEntity refVersion = versionRepository.getOne(reference.getReferenceVersion());
        Field draftField = field(draftVersion.getStructure().getAttribute(reference.getAttribute()));
        Field refField = field(refVersion.getStructure().getAttribute(reference.getReferenceAttribute()));

        // значения, которые невозможно привести к типу атрибута, на который ссылаемся, либо не найдены в ссылаемой версии
        List<String> incorrectValues = new ArrayList<>();
        List<Message> messages = new ArrayList<>();

        DataCriteria draftDataCriteria = new DataCriteria(draftVersion.getStorageCode(), null, null,
                singletonList(draftField), emptySet(), null);
        draftDataCriteria.setPage(1);
        draftDataCriteria.setSize(bufSize);
        validateData(draftDataCriteria, incorrectValues, refField, refVersion);

        incorrectValues.forEach(incorrectValue ->
                messages.add(
                        new Message(INCONVERTIBLE_DATA_TYPES_EXCEPTION_CODE,
                                draftVersion.getStructure().getAttribute(reference.getAttribute()).getDescription(),
                                incorrectValue)
                )
        );
        return messages;
    }

    private void validateData(DataCriteria draftDataCriteria, List<String> incorrectValues, Field refField, RefBookVersionEntity refVersion) {
        CollectionPage<RowValue> draftRowValues = searchDataService.getPagedData(draftDataCriteria);
        // значения, которые приведены к типу атрибута из ссылки
        List<Object> castedValues = new ArrayList<>();

        (draftRowValues.getCollection()).forEach(rowValue -> {
            String value = String.valueOf(rowValue.getFieldValue(reference.getAttribute()).getValue());
            Object castedValue;
            try {
                castedValue = ConverterUtil.castReferenceValue(refField, value);
                castedValues.add(castedValue);
            } catch (NumberFormatException | DateTimeParseException | RdmException e) {
                incorrectValues.add(value);
                logger.error("Can not parse value " + value, e);
            }
        });
        if (!isEmpty(castedValues)) {
            FieldSearchCriteria refFieldSearchCriteria = new FieldSearchCriteria(refField, SearchTypeEnum.EXACT, castedValues);
            DataCriteria refDataCriteria =
                    new DataCriteria(refVersion.getStorageCode(),
                            date(refVersion.getFromDate()), date(refVersion.getToDate()),
                            singletonList(refField), singletonList(refFieldSearchCriteria), null);
            CollectionPage<RowValue> refRowValues = searchDataService.getPagedData(refDataCriteria);
            castedValues.forEach(castedValue -> {
                if (refRowValues.getCollection().stream()
                        .noneMatch(rowValue ->
                                castedValue.equals(rowValue.getFieldValue(reference.getReferenceAttribute()).getValue())
                        ))
                    incorrectValues.add(String.valueOf(castedValue));
            });
        }
        int remainCount = draftRowValues.getCount() - (draftDataCriteria.getPage() - 1) * bufSize - draftDataCriteria.getSize();
        if (remainCount <= 0)
            return;
        draftDataCriteria.setPage(draftDataCriteria.getPage() + 1);
        draftDataCriteria.setSize((remainCount >= bufSize) ? bufSize : remainCount);
        validateData(draftDataCriteria, incorrectValues, refField, refVersion);
    }
}
