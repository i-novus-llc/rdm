package ru.inovus.ms.rdm.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.enumeration.RefBookStatusType;
import ru.inovus.ms.rdm.model.version.ReferrerVersionCriteria;
import ru.inovus.ms.rdm.repository.RefBookVersionRepository;

import java.util.List;

import static java.util.Collections.singletonList;

public class ReferrerEntityIteratorProvider {

    private static final int REF_BOOK_VERSION_PAGE_SIZE = 100;

    private static final String VERSION_ID_SORT_PROPERTY = "id";

    private static final List<Sort.Order> SORT_REFERRER_VERSIONS = singletonList(
            new Sort.Order(Sort.Direction.ASC, VERSION_ID_SORT_PROPERTY)
    );

    private RefBookVersionRepository versionRepository;
    private ReferrerVersionCriteria criteria;

    public ReferrerEntityIteratorProvider(RefBookVersionRepository versionRepository,
                                          String refBookCode, RefBookSourceType sourceType) {
        this.versionRepository = versionRepository;

        this.criteria = new ReferrerVersionCriteria(refBookCode, RefBookStatusType.USED, sourceType);
        criteria.setOrders(SORT_REFERRER_VERSIONS);
        criteria.setPageSize(REF_BOOK_VERSION_PAGE_SIZE);
    }

    public Page<RefBookVersionEntity> search(ReferrerVersionCriteria criteria) {
        PageRequest pageRequest = PageRequest.of(criteria.getPageNumber(), criteria.getPageSize());
        return versionRepository.findReferrerVersions(criteria.getRefBookCode(), criteria.getStatusType().name(), criteria.getSourceType().name(), pageRequest);
    }

    public PageIterator<RefBookVersionEntity, ReferrerVersionCriteria> iterate() {
        return new PageIterator<>(this::search, criteria);
    }
}
