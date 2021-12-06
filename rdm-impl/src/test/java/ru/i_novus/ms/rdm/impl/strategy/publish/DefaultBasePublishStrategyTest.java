package ru.i_novus.ms.rdm.impl.strategy.publish;

import com.querydsl.core.types.Predicate;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.VersionNumberStrategy;
import ru.i_novus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.file.export.PerRowFileGenerator;
import ru.i_novus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.service.RefBookLockService;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus.PUBLISHED;

@RunWith(MockitoJUnitRunner.class)
public class DefaultBasePublishStrategyTest {

    private static final int REFBOOK_ID = 2;
    private static final String REFBOOK_CODE = "refbook_code";

    private static final String DRAFT_STORAGE_CODE = "draft-storage-code";
    private static final String PUBLISHED_STORAGE_CODE = "published-storage-code";

    @InjectMocks
    private DefaultBasePublishStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private DraftDataService draftDataService;
    @Mock
    private SearchDataService searchDataService;
    @Mock
    private DropDataService dropDataService;

    @Mock
    private RefBookLockService refBookLockService;

    @Mock
    private VersionService versionService;
    @Mock
    private ConflictService conflictService;

    @Mock
    private VersionFileService versionFileService;
    @Mock
    private VersionFileRepository versionFileRepository;
    @Mock
    private VersionNumberStrategy versionNumberStrategy;

    @Mock
    private VersionValidation versionValidation;
    @Mock
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    @Mock
    private PerRowFileGenerator perRowFileGenerator;
    @Mock
    private PerRowFileGeneratorFactory fileGeneratorFactory;

    @Mock
    private PassportValueRepository passportValueRepository;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private AsyncOperationQueue asyncQueue;

    @Mock
    private AfterPublishStrategy afterPublishStrategy;

    @Before
    public void setUp() {

        when(draftDataService.applyDraft(any(), any(), any(), any())).thenReturn(PUBLISHED_STORAGE_CODE);
        when(searchDataService.hasData(any())).thenReturn(true);
    }

    @Test
    @SuppressWarnings("java:S5778")
    public void testPublishFirstDraft() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        String expectedDraftStorageCode = draftEntity.getStorageCode();

        RefBookVersionEntity expected = createDraftEntity();
        expected.setVersion("1.1");
        expected.setStatus(PUBLISHED);
        expected.setStorageCode(PUBLISHED_STORAGE_CODE);
        LocalDateTime fromDate = LocalDateTime.now();
        expected.setFromDate(fromDate);

        RefBookVersion draftVersion = ModelGenerator.versionModel(draftEntity);
        when(versionService.getById(eq(draftEntity.getId()))).thenReturn(draftVersion);
        when(versionNumberStrategy.next(eq(REFBOOK_ID))).thenReturn("1.1");
        when(versionNumberStrategy.check(eq("1.1"), eq(REFBOOK_ID))).thenReturn(false);

        // Invalid versionName
        try {
            publish(draftEntity, "1.1", fromDate, null, false);
            fail();

        } catch (UserException e) {
            assertEquals("invalid.version.name", e.getCode());
            assertEquals("1.1", e.getArgs()[0]);
        }

        // Invalid version period
        try {
            publish(draftEntity, null, fromDate, LocalDateTime.MIN, false);
            fail();

        } catch (UserException e) {
            assertEquals("invalid.version.period", e.getCode());
        }

        // Without version name
        PublishResponse result = publish(draftEntity, null, fromDate, null, false);
        assertEquals("1.1", draftEntity.getVersion());

        assertNotNull(result);
        assertEquals(REFBOOK_CODE, result.getRefBookCode());
        assertNull(result.getOldId());
        assertEquals(expected.getId(), result.getNewId());

        verify(draftDataService).applyDraft(isNull(), eq(expectedDraftStorageCode), eq(fromDate), any());

        ArgumentCaptor<RefBookVersionEntity> savedCaptor = ArgumentCaptor.forClass(RefBookVersionEntity.class);
        verify(versionRepository).save(savedCaptor.capture());

        expected.setLastActionDate(savedCaptor.getValue().getLastActionDate());
        assertEquals(expected, savedCaptor.getValue());

        verify(versionFileService, times(2)).save(eq(draftVersion), any(FileType.class), eq(null));
    }

    @Test
    public void testPublishNextVersionWithSameStructure() {

        RefBookVersionEntity baseEntity = createBaseEntity();
        baseEntity.setVersion("2.1");

        RefBookVersionEntity draftEntity = createDraftEntity();
        draftEntity.setStructure(baseEntity.getStructure());

        String expectedDraftStorageCode = draftEntity.getStorageCode();

        RefBookVersionEntity expected = createDraftEntity();
        expected.setVersion("2.2");
        expected.setStatus(PUBLISHED);
        expected.setStorageCode(PUBLISHED_STORAGE_CODE);
        LocalDateTime fromDate = LocalDateTime.now();
        expected.setFromDate(fromDate);

        when(versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(anyInt(), eq(PUBLISHED)))
                .thenReturn(baseEntity);

        when(versionService.getById(eq(draftEntity.getId()))).thenReturn(ModelGenerator.versionModel(draftEntity));
        when(versionNumberStrategy.check("2.2", REFBOOK_ID)).thenReturn(true);

        PublishResponse result = publish(draftEntity, expected.getVersion(), fromDate, null, false);

        assertNotNull(result);
        assertEquals(REFBOOK_CODE, result.getRefBookCode());
        assertEquals(baseEntity.getId(), result.getOldId());
        assertEquals(expected.getId(), result.getNewId());

        verify(draftDataService)
                .applyDraft(baseEntity.getStorageCode(), expectedDraftStorageCode, fromDate, null);

        ArgumentCaptor<RefBookVersionEntity> savedCaptor = ArgumentCaptor.forClass(RefBookVersionEntity.class);
        verify(versionRepository).save(savedCaptor.capture());

        expected.setLastActionDate(savedCaptor.getValue().getLastActionDate());
        assertEquals(expected, savedCaptor.getValue());
    }

    @Test
    public void testPublishWithAllOverlappingCases() {

        List<RefBookVersionEntity> actual = getVersionsForOverlappingPublish();
        List<RefBookVersionEntity> expected = getExpectedAfterOverlappingPublish();

        RefBookVersionEntity draftEntity = createDraftEntity();

        when(versionRepository.findAll(any(Predicate.class))).thenReturn(new PageImpl<>(actual));

        when(versionService.getById(eq(draftEntity.getId()))).thenReturn(ModelGenerator.versionModel(draftEntity));
        when(versionNumberStrategy.check(eq("2.4"), eq(REFBOOK_ID))).thenReturn(true);
        doAnswer(invocation -> actual.removeIf(e -> e.getId().equals((invocation.getArguments()[0]))))
                .when(versionRepository).deleteById(anyInt());

        publish(draftEntity, "2.4",
                LocalDateTime.of(2017, 1, 4, 1, 1),
                LocalDateTime.of(2017, 1, 9, 1, 1),
                false);
        assertEquals(expected, actual);
    }

    private List<RefBookVersionEntity> getVersionsForOverlappingPublish() {

        return new ArrayList<>(asList(
                createVersionEntity(REFBOOK_ID, 2, PUBLISHED,
                        LocalDateTime.of(2017, 1, 3, 1, 1),
                        LocalDateTime.of(2017, 1, 5, 1, 1)),
                createVersionEntity(REFBOOK_ID, 3, PUBLISHED,
                        LocalDateTime.of(2017, 1, 6, 1, 1),
                        LocalDateTime.of(2017, 1, 7, 1, 1)),
                createVersionEntity(REFBOOK_ID, 4, PUBLISHED,
                        LocalDateTime.of(2017, 1, 8, 1, 1),
                        LocalDateTime.of(2017, 1, 10, 1, 1))
        ));
    }

    private List<RefBookVersionEntity> getExpectedAfterOverlappingPublish() {

        return singletonList(
                createVersionEntity(REFBOOK_ID, 2, PUBLISHED,
                        LocalDateTime.of(2017, 1, 3, 1, 1),
                        LocalDateTime.of(2017, 1, 4, 1, 1))
        );
    }

    private RefBookVersionEntity createVersionEntity(Integer refBookId, Integer versionId, RefBookVersionStatus status,
                                                     LocalDateTime fromDate, LocalDateTime toDate) {
        RefBookVersionEntity versionEntity = new RefBookVersionEntity();

        RefBookEntity refBookEntity = new DefaultRefBookEntity();
        refBookEntity.setId(refBookId);
        versionEntity.setRefBook(refBookEntity);

        versionEntity.setId(versionId);
        versionEntity.setStatus(status);
        versionEntity.setFromDate(fromDate);
        versionEntity.setToDate(toDate);

        return versionEntity;
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFBOOK_ID);
        entity.setCode(REFBOOK_CODE);

        return entity;
    }

    private RefBookVersionEntity createDraftEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(1);
        entity.setStorageCode(DRAFT_STORAGE_CODE);
        entity.setRefBook(createRefBookEntity());
        entity.setStatus(RefBookVersionStatus.DRAFT);
        entity.setStructure(createStructure());
        entity.setPassportValues(createPassportValues(entity));

        return entity;
    }

    private RefBookVersionEntity createBaseEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(3);
        entity.setStorageCode("base-storage-code");
        entity.setRefBook(createRefBookEntity());
        entity.setStatus(PUBLISHED);
        entity.setStructure(createStructure());
        entity.setPassportValues(createPassportValues(entity));

        return entity;
    }

    private Structure createStructure() {

        return new Structure(
                asList(
                        Structure.Attribute.build("Code", "Код", FieldType.INTEGER, "Код записи"),
                        Structure.Attribute.build("Name", "Имя", FieldType.STRING, "Наименование"),
                        Structure.Attribute.build("Rec_Date", "Дата", FieldType.DATE, "Дата записи"),
                        Structure.Attribute.build("Count", "Счётчик", FieldType.FLOAT, "Количество")
                ),
                null);
    }

    private List<PassportValueEntity> createPassportValues(RefBookVersionEntity version) {

        List<PassportValueEntity> passportValues = new ArrayList<>();
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("fullName"), "full_name", version));
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("shortName"), "short_name", version));
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("annotation"), "annotation", version));

        return passportValues;
    }

    private PublishResponse publish(RefBookVersionEntity entity, String versionName,
                                    LocalDateTime fromDate, LocalDateTime toDate,
                                    boolean resolveConflicts) {
        PublishRequest request = new PublishRequest(null);
        request.setVersionName(versionName);
        request.setFromDate(fromDate);
        request.setToDate(toDate);
        request.setResolveConflicts(resolveConflicts);

        return strategy.publish(entity, request);
    }
}