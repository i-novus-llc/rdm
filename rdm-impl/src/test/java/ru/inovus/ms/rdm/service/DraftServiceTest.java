package ru.inovus.ms.rdm.service;

import com.querydsl.core.types.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.Draft;
import ru.inovus.ms.rdm.model.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.isPublished;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.isVersionOfRefBook;

/**
 * Created by tnurdinov on 21.06.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class DraftServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";
    private static final String TEST_DRAFT_CODE = "test_draft_code";
    private static final String TEST_DRAFT_CODE_NEW = "test_draft_code_new";

    @InjectMocks
    private DraftServiceImpl draftService;

    @Mock
    RefBookVersionRepository versionRepository;

    @Mock
    DraftDataService draftDataService;

    @Mock
    FieldFactory fieldFactory;

    @Mock
    DropDataService dropDataService;

    @Mock
    RefBookRepository refBookRepository;

    @Before
    public void setUp() throws Exception {
        reset(draftDataService);
        when(draftDataService.applyDraft(any(), any(), any())).thenReturn(TEST_STORAGE_CODE);
        when(draftDataService.createDraft(anyList())).thenReturn(TEST_DRAFT_CODE_NEW);
    }

    @Test
    public void testPublishFirstDraft() throws Exception {

        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        String expectedDraftStorageCode = testDraftVersion.getStorageCode();
        RefBookVersionEntity expectedVersionEntity = createTestDraftVersion();
        expectedVersionEntity.setVersion("1.0");
        expectedVersionEntity.setStatus(RefBookVersionStatus.PUBLISHED);
        expectedVersionEntity.setStorageCode(TEST_STORAGE_CODE);


        when(versionRepository.findOne(eq(testDraftVersion.getId()))).thenReturn(testDraftVersion);
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(null);
        OffsetDateTime now = OffsetDateTime.now();
        draftService.publish(testDraftVersion.getId(), "1.0", now);

        verify(draftDataService).applyDraft(isNull(String.class), eq(expectedDraftStorageCode), eq(new Date(now.toInstant().toEpochMilli())));
        verify(versionRepository).save(eq(expectedVersionEntity));
        reset(versionRepository);
    }


    @Test
    public void testCreateWithExistingDraftSameStructure() throws Exception {
        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        when(versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, 2)).thenReturn(testDraftVersion);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(testDraftVersion);
        assertEquals(new Draft(1, TEST_DRAFT_CODE), draftService.create(2, testDraftVersion.getStructure()));
        reset(versionRepository);
    }

    @Test
    public void testCreateWithExistingDraftDifferentStructure() throws Exception {
        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        String oldStorageCode = testDraftVersion.getStorageCode();
        when(versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, 2)).thenReturn(testDraftVersion);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(testDraftVersion);
        when(fieldFactory.createField(any(), any())).thenReturn(null);
        doNothing().when(dropDataService).drop(any());
        Structure structure = new Structure();
        structure.setAttributes(Collections.singletonList(Structure.Attribute.build("name", FieldType.STRING, true)));
        Draft draftActual = draftService.create(2, structure);
        assertEquals(testDraftVersion.getId(), draftActual.getId());
        assertNotEquals(TEST_DRAFT_CODE, draftActual.getStorageCode());
        reset(versionRepository);
    }

    @Test
    public void testCreateWithoutDraftWithPublishedVersion() throws Exception {
        when(versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, 2)).thenReturn(null);
        RefBookEntity refBook = new RefBookEntity();
        when(refBookRepository.findOne(anyInt())).thenReturn(refBook);
        Structure structure = new Structure();
        RefBookVersionEntity expectedRefBookVersion = new RefBookVersionEntity();
        expectedRefBookVersion.setStorageCode(TEST_DRAFT_CODE_NEW);
        expectedRefBookVersion.setShortName("short_name");
        expectedRefBookVersion.setFullName("full_name");
        expectedRefBookVersion.setAnnotation("annotation");
        expectedRefBookVersion.setStructure(structure);
        expectedRefBookVersion.setRefBook(refBook);
        expectedRefBookVersion.setStatus(RefBookVersionStatus.DRAFT);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(expectedRefBookVersion);
        RefBookVersionEntity lastRefBookVersion = createTestPublishedVersion();
        Page<RefBookVersionEntity> lastRefBookVersionPage = new PageImpl<>(Collections.singletonList(lastRefBookVersion));
        when(versionRepository
                .findAll(isPublished().and(isVersionOfRefBook(2))
                        , new PageRequest(1, 1, new Sort(Sort.Direction.DESC, "fromDate")))).thenReturn(lastRefBookVersionPage);
        draftService.create(2, structure);
        verify(versionRepository).save(eq(expectedRefBookVersion));
        reset(versionRepository);
    }

    @Test
    public void testRemoveDraft() {
        draftService.remove(1);
        verify(versionRepository).delete(anyInt());
    }

    private RefBookVersionEntity createTestDraftVersion() {
        RefBookVersionEntity testDraftVersion = new RefBookVersionEntity();
        testDraftVersion.setId(1);
        testDraftVersion.setStorageCode(TEST_DRAFT_CODE);
        testDraftVersion.setRefBook(createTestRefBook());
        testDraftVersion.setStatus(RefBookVersionStatus.DRAFT);
        testDraftVersion.setStructure(new Structure());
        return testDraftVersion;
    }

    private RefBookVersionEntity createTestPublishedVersion() {
        RefBookVersionEntity testDraftVersion = new RefBookVersionEntity();
        testDraftVersion.setId(3);
        testDraftVersion.setStorageCode("testVersionStorageCode");
        testDraftVersion.setRefBook(createTestRefBook());
        testDraftVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        testDraftVersion.setStructure(new Structure());
        testDraftVersion.setShortName("short_name");
        testDraftVersion.setFullName("full_name");
        testDraftVersion.setAnnotation("annotation");
        return testDraftVersion;
    }

    private RefBookEntity createTestRefBook() {
        RefBookEntity testRefBook = new RefBookEntity();
        testRefBook.setId(2);
        testRefBook.setCode("test_ref_book");
        return testRefBook;
    }

    private RefBookVersionEntity eqRefBookVersionEntity(RefBookVersionEntity refBookVersionEntity) {
        return argThat(new RefBookVersionEntityMatcher(refBookVersionEntity));
    }

    private static class RefBookVersionEntityMatcher extends ArgumentMatcher<RefBookVersionEntity> {

        private RefBookVersionEntity expected;

        public RefBookVersionEntityMatcher(RefBookVersionEntity versionEntity) {
            this.expected = versionEntity;
        }

        @Override
        public boolean matches(Object actual) {
            if (!(actual instanceof RefBookVersionEntity)) {
                return false;
            }

            RefBookVersionEntity actualVersion = ((RefBookVersionEntity) actual);
            return expected.equals(actualVersion);
        }
    }
}
