package ru.i_novus.ms.rdm.impl.model.refdata;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toReferenceSearchCriterias;

/**
 * Критерий поиска данных в ссылочном справочнике
 * по значениям первичных ключей исходного справочника.
 */
public class ReferrerDataCriteria extends StorageDataCriteria {

    /**
     * Размер страницы данных ссылочного справочника.
     * <p/>
     * Совпадает со значением ConflictServiceImpl.REF_BOOK_VERSION_DATA_PAGE_SIZE.
     */
    private static final int REFERRER_DATA_PAGE_SIZE = 100;

    /**
     * Конструктор критерия.
     *
     * @param referrer      версия справочника, который ссылается
     * @param references    список атрибутов-ссылок
     * @param storageCode   код хранилища версии
     * @param primaryValues значения первичных ключей
     */
    public ReferrerDataCriteria(RefBookVersionEntity referrer,
                                List<Structure.Reference> references,
                                String storageCode,
                                List<String> primaryValues) {

        super(storageCode, referrer.getFromDate(), referrer.getToDate(),
                references.stream().map(ConverterUtil::field).collect(toList()));

        if (!isEmpty(primaryValues)) {
            setFieldFilters(toReferenceSearchCriterias(references, primaryValues));
        }

        setPage(DataCriteria.MIN_PAGE);
        setSize(REFERRER_DATA_PAGE_SIZE);
    }
}
