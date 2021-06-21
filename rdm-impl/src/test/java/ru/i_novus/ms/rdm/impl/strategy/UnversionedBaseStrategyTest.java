package ru.i_novus.ms.rdm.impl.strategy;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookStatusType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.ReferrerVersionCriteria;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.*;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.NAME_ATTRIBUTE_CODE;

public abstract class UnversionedBaseStrategyTest extends DefaultBaseStrategyTest {

    private static final String USED_VERSION = "-1.0";

    protected static final int REFERRER_ID = 10;
    protected static final String REFERRER_CODE = "refer";

    protected static final int REFERRER_VERSION_ID = 33;
    protected static final String REFERRER_VERSION_CODE = "refer_code";
    protected static final String REFERRER_ATTRIBUTE_CODE = "ref";

    protected static final long REFERRER_SYSTEM_ID_MULTIPLIER = 10L;

    private static final RefBookStatusType FIND_REFERRERS_STATUS = RefBookStatusType.USED;
    private static final RefBookSourceType FIND_REFERRERS_SOURCE = RefBookSourceType.ALL;
    private static final ReferrerVersionCriteria FIND_REFERRERS_CRITERIA = new ReferrerVersionCriteria(REFBOOK_CODE, FIND_REFERRERS_STATUS, FIND_REFERRERS_SOURCE);

    protected RefBookVersionEntity createUnversionedEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setRefBook(createRefBookEntity());
        entity.setStructure(createStructure());
        entity.setStorageCode(DRAFT_CODE);
        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        entity.setVersion(USED_VERSION);
        entity.setFromDate(TimeUtils.now());

        return entity;
    }

    protected void mockFindReferrers(RefBookVersionRepository versionRepository, List<RefBookVersionEntity> referrers) {

        when(versionRepository.findReferrerVersions(
                eq(REFBOOK_CODE),
                eq(FIND_REFERRERS_STATUS.name()),
                eq(FIND_REFERRERS_SOURCE.name()),
                any(Pageable.class))
        )
                .thenReturn(new PageImpl<>(referrers, FIND_REFERRERS_CRITERIA, 1)) // referrers
                .thenReturn(new PageImpl<>(emptyList(), FIND_REFERRERS_CRITERIA, 1)); // stop
    }

    protected void verifyFindReferrers(RefBookVersionRepository versionRepository) {

        verify(versionRepository, times(2)).findReferrerVersions(
                eq(REFBOOK_CODE),
                eq(FIND_REFERRERS_STATUS.name()),
                eq(FIND_REFERRERS_SOURCE.name()),
                any(Pageable.class)
        );
    }

    protected RefBookVersionEntity createReferrerVersionEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(REFERRER_VERSION_ID);
        entity.setRefBook(createReferrerEntity());
        entity.setStructure(createReferrerStructure());
        entity.setStorageCode(REFERRER_VERSION_CODE);
        entity.setStatus(RefBookVersionStatus.PUBLISHED);

        return entity;
    }

    protected RefBookEntity createReferrerEntity() {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFERRER_ID);
        entity.setCode(REFERRER_CODE);

        return entity;
    }

    protected Structure createReferrerStructure() {

        String referenceDisplayExpression = DisplayExpression.toPlaceholder(NAME_ATTRIBUTE_CODE);
        Structure.Attribute refAttribute = Structure.Attribute.build(REFERRER_ATTRIBUTE_CODE, REFERRER_ATTRIBUTE_CODE, FieldType.REFERENCE, null);
        Structure.Reference refReference = new Structure.Reference(REFERRER_ATTRIBUTE_CODE, REFBOOK_CODE, referenceDisplayExpression);

        Structure structure = new Structure();
        structure.add(refAttribute, refReference);

        return structure;
    }

    protected LongRowValue createReferrerRowValue(Long systemId, Integer referredId) {

        Reference reference = new Reference(referredId.toString(), NAME_FIELD_VALUE_PREFIX + referredId);

        List<FieldValue> fieldValues = singletonList(
                new ReferenceFieldValue(REFERRER_ATTRIBUTE_CODE, reference)
        );

        return new LongRowValue(systemId, fieldValues);
    }
}
