package ru.i_novus.ms.rdm.impl.model.refdata;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;

import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.FieldValueUtils.castReferenceValue;

/**
 * Критерий поиска данных в исходном справочнике,
 * по значениям атрибута-ссылки из ссылочного справочника.
 */
public class ReferredDataCriteria extends StorageDataCriteria {

    /**
     * Конструктор критерия.
     *
     * @param entity          версия справочника, на который ссылаются
     * @param primaries       список первичных ключей
     * @param storageCode     код хранилища версии
     * @param fieldAttributes список атрибутов как возвращаемых полей
     * @param referenceValues значения атрибута-ссылки
     */
    public ReferredDataCriteria(RefBookVersionEntity entity,
                                List<Structure.Attribute> primaries,
                                String storageCode,
                                List<Structure.Attribute> fieldAttributes,
                                List<String> referenceValues) {

        super(storageCode, entity.getFromDate(), entity.getToDate(), ConverterUtil.fields(fieldAttributes));

        if (!isEmpty(referenceValues)) {
            setFieldFilters(toPrimarySearchCriterias(primaries, referenceValues));
        }

        setPage(BaseDataCriteria.MIN_PAGE);
        setSize(referenceValues != null ? referenceValues.size() : 0);
    }

    private Set<List<FieldSearchCriteria>> toPrimarySearchCriterias(List<Structure.Attribute> primaries,
                                                                    List<String> referenceValues) {
        return referenceValues.stream()
                .map(refValue -> toPrimarySearchCriterias(primaries, refValue))
                .collect(toSet());
    }

    private List<FieldSearchCriteria> toPrimarySearchCriterias(List<Structure.Attribute> primaries,
                                                               String referenceValue) {
        // На данный момент первичным ключом может быть только одно поле.
        // Ссылка на значение составного ключа невозможна.
        Structure.Attribute primary = primaries.get(0);

        return singletonList(
                ConverterUtil.toFieldSearchCriteria(primary.getCode(), primary.getType(), SearchTypeEnum.EXACT,
                        singletonList(castReferenceValue(referenceValue, primary.getType()))
                )
        );
    }
}
