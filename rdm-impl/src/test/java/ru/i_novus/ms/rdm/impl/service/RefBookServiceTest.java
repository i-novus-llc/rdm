package ru.i_novus.ms.rdm.impl.service;

import com.querydsl.core.types.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookVersionQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefBookServiceTest {

    @InjectMocks
    private RefBookServiceImpl refBookService;

    @Mock
    private RefBookRepository refBookRepository;
    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookModelDataRepository refBookModelDataRepository;

    @Mock
    private DropDataService dropDataService;

    @Mock
    private RefBookLockService refBookLockService;

    @Mock
    private PassportValueRepository passportValueRepository;
    @Mock
    private RefBookVersionQueryProvider refBookVersionQueryProvider;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private DraftService draftService;
    @Mock
    private PublishService publishService;

    @Mock
    private AuditLogService auditLogService;

    @Test
    public void testSearch() {

        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setExcludeDraft(true);

        RefBookEntity refBookEntity1 = createRefBookEntity(1);
        RefBookEntity refBookEntity2 = createRefBookEntity(2);
        List<RefBookEntity> refBookEntities = List.of(refBookEntity1, refBookEntity2);
        List<RefBookVersionEntity> versionEntities = refBookEntities.stream()
                .map(this::createRefBookVersionEntity)
                .collect(toList());

        // .search
        when(refBookVersionQueryProvider.search(criteria))
                .thenReturn(new PageImpl<>(versionEntities, criteria, versionEntities.size()));

        // .getSourceTypeVersions
        RefBookCriteria versionCriteria = new RefBookCriteria();
        versionCriteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);
        versionCriteria.setRefBookIds(refBookEntities.stream().map(RefBookEntity::getId).collect(toList()));
        when(refBookVersionQueryProvider.toPredicate(versionCriteria)).thenReturn(null);

        List<RefBookVersionEntity> lastPublishedEntities = new ArrayList<>(versionEntities);
        PageRequest versionRequest = PageRequest.of(0, refBookEntities.size());
        when(versionRepository.findAll(Mockito.<Predicate>any(), eq(versionRequest)))
                .thenReturn(new PageImpl<>(lastPublishedEntities, criteria, lastPublishedEntities.size()));

        // .refBookModel
        when(refBookModelDataRepository.findData(any(Integer.class), any(Boolean.class), any(Integer.class)))
                .thenAnswer(v -> {
                    RefBookModelData data = new RefBookModelData();

                    Integer currentVersionId = (Integer) v.getArguments()[0];
                    data.setCurrentVersionId(currentVersionId);

                    RefBookVersionEntity lastPublishedVersion = lastPublishedEntities.stream()
                            .filter(entity -> Objects.equals(currentVersionId, entity.getId()))
                            .findFirst().orElse(null);
                    data.setLastPublishedVersion(lastPublishedVersion);

                    return data;
                });

        Page<RefBook> refBooks = refBookService.search(criteria);
        assertEquals(refBookEntities.size(), refBooks.getTotalElements());
        assertNotNull(refBooks.getContent());

        refBooks.getContent().forEach(actual -> {

            RefBookVersionEntity expected = versionEntities.stream()
                    .filter(entity -> Objects.equals(actual.getId(), entity.getId()))
                    .findFirst().orElse(null);
            assertNotNull(expected);
            assertEquals(expected.getRefBook().getId(), actual.getRefBookId());

            RefBookVersionEntity lastPublishedVersion = lastPublishedEntities.stream()
                    .filter(entity -> Objects.equals(actual.getLastPublishedVersionId(), entity.getId()))
                    .findFirst().orElse(null);
            assertNotNull(lastPublishedVersion);
        });
    }

    @Test
    public void testGetByVersionId() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        RefBookVersionEntity versionEntity = createRefBookVersionEntity(refBookEntity);

        // .findVersionOrThrow
        when(versionRepository.findById(10)).thenReturn(Optional.of(versionEntity));

        // .hasReferrerVersions
        when(versionRepository.existsReferrerVersions(eq(refBookEntity.getCode()), any(String.class), any(String.class)))
                .thenReturn(Boolean.FALSE);

        // .getSourceTypeVersion
        when(versionRepository.findAll(Mockito.<Predicate>any(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(emptyList(), Pageable.unpaged(), 0));

        // .refBookModel
        when(refBookModelDataRepository.findData(any(Integer.class), any(Boolean.class), any(Integer.class)))
                .thenReturn(new RefBookModelData());

        RefBook actual = refBookService.getByVersionId(versionEntity.getId());
        assertNotNull(actual);
    }

    @Test
    public void testGetCode() {

        RefBookEntity refBookEntity = createRefBookEntity(1);

        when(refBookRepository.getOne(refBookEntity.getId())).thenReturn(refBookEntity);

        String refBookCode = refBookService.getCode(refBookEntity.getId());
        assertEquals(refBookEntity.getCode(), refBookCode);
    }

    @Test
    public void testGetId() {

        RefBookEntity refBookEntity = createRefBookEntity(1);

        when(refBookRepository.findByCode(refBookEntity.getCode())).thenReturn(refBookEntity);

        Integer refBookId = refBookService.getId(refBookEntity.getCode());
        assertEquals(refBookEntity.getId(), refBookId);
    }

    private RefBookEntity createRefBookEntity(Integer id) {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(id);
        entity.setCode("code_" + id);

        return entity;
    }

    private RefBookVersionEntity createRefBookVersionEntity(RefBookEntity refBookEntity) {

        return createRefBookVersionEntity(refBookEntity.getId() * 10, refBookEntity);
    }

    private RefBookVersionEntity createRefBookVersionEntity(Integer id, RefBookEntity refBookEntity) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(id);
        entity.setRefBook(refBookEntity);

        return entity;
    }
}