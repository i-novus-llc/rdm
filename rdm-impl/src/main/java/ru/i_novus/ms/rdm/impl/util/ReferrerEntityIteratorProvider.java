package ru.i_novus.ms.rdm.impl.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookStatusType;
import ru.i_novus.ms.rdm.api.model.version.ReferrerVersionCriteria;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.util.List;

import static java.util.Collections.singletonList;

public class ReferrerEntityIteratorProvider {

    private static final int REF_BOOK_VERSION_PAGE_SIZE = 100;

    private static final String VERSION_ID_SORT_PROPERTY = "id";

    private static final List<Sort.Order> SORT_REFERRER_VERSIONS = singletonList(
            new Sort.Order(Sort.Direction.ASC, VERSION_ID_SORT_PROPERTY)
    );

    private final RefBookVersionRepository versionRepository;
    private final ReferrerVersionCriteria criteria;

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
