package ru.inovus.ms.rdm.service;

import com.querydsl.core.types.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Pageable;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.time.OffsetDateTime;
import java.util.Date;

import static org.mockito.Mockito.*;

/**
 * Created by tnurdinov on 21.06.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class DraftServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";

    @InjectMocks
    private DraftServiceImpl draftService;

    @Mock
    RefBookVersionRepository versionRepository;

    @Mock
    DraftDataService draftDataService;

    @Before
    public void setUp() throws Exception {
        reset(draftDataService);
        when(draftDataService.applyDraft(any(), any(), any())).thenReturn(TEST_STORAGE_CODE);
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
        verify(versionRepository).save(eqRefBookVersionEntity(expectedVersionEntity));
        reset(versionRepository);
    }

    private RefBookVersionEntity createTestDraftVersion() {
        RefBookVersionEntity testDraftVersion = new RefBookVersionEntity();
        testDraftVersion.setId(1);
        testDraftVersion.setStorageCode("testDraftCode");
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
            if(!(actual instanceof RefBookVersionEntity)) {
                return false;
            }

            RefBookVersionEntity actualVersion = ((RefBookVersionEntity) actual);
            return expected.equals(actualVersion);
        }
    }
}
