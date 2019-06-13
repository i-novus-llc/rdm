package ru.inovus.ms.rdm.service;

import com.querydsl.core.types.Predicate;
import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.file.MockFileStorage;
import ru.inovus.ms.rdm.file.export.PerRowFileGenerator;
import ru.inovus.ms.rdm.file.export.PerRowFileGeneratorFactory;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.*;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.VersionFileService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.FileNameGenerator;
import ru.inovus.ms.rdm.util.ModelGenerator;
import ru.inovus.ms.rdm.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.validation.VersionPeriodPublishValidation;
import ru.inovus.ms.rdm.validation.VersionValidation;
import ru.inovus.ms.rdm.validation.VersionValidationImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;

@RunWith(MockitoJUnitRunner.class)
public class PublishServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";
    private static final String TEST_DRAFT_CODE = "test_draft_code";
    private static final String TEST_DRAFT_CODE_NEW = "test_draft_code_new";
    private static final String TEST_REF_BOOK = "test_ref_book";
    private static final int REFBOOK_ID = 2;

    @InjectMocks
    private PublishServiceImpl publishService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private DraftDataService draftDataService;
    @Mock
    private DropDataService dropDataService;

    @Mock
    private RefBookLockService refBookLockService;
    @Mock
    private VersionService versionService;
    @Mock
    private ConflictService conflictService;

    @Spy
    private FileStorage fileStorage = new MockFileStorage();
    @Mock
    private FileNameGenerator fileNameGenerator;

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

    @Before
    public void setUp() {
        reset(draftDataService, fileNameGenerator, fileGeneratorFactory);
        when(draftDataService.applyDraft(any(), any(), any(), any())).thenReturn(TEST_STORAGE_CODE);

//        when(fileNameGenerator.generateName(any(RefBookVersion.class), eq(FileType.XLSX))).thenReturn("version.xlsx");
//        when(fileNameGenerator.generateZipName(any(RefBookVersion.class), eq(FileType.XLSX))).thenReturn("version_xlsx.zip");
//        when(fileNameGenerator.generateName(any(RefBookVersion.class), eq(FileType.XML))).thenReturn("version.xml");
//        when(fileNameGenerator.generateZipName(any(RefBookVersion.class), eq(FileType.XML))).thenReturn("version_xml.zip");
//        when(fileGeneratorFactory.getFileGenerator(any(Iterator.class), any(RefBookVersion.class), any(FileType.class)))
//                .thenReturn(perRowFileGenerator);
    }

    @Test
    public void testPublishFirstDraft() {

        RefBookVersionEntity draftVersionEntity = createTestDraftVersionEntity();
        String expectedDraftStorageCode = draftVersionEntity.getStorageCode();

        RefBookVersionEntity expectedVersionEntity = createTestDraftVersionEntity();
        expectedVersionEntity.setVersion("1.1");
        expectedVersionEntity.setStatus(RefBookVersionStatus.PUBLISHED);
        expectedVersionEntity.setStorageCode(TEST_STORAGE_CODE);
        LocalDateTime now = LocalDateTime.now();
        expectedVersionEntity.setFromDate(now);

        when(versionRepository.getOne(eq(draftVersionEntity.getId()))).thenReturn(draftVersionEntity);
        when(versionRepository.findById(eq(draftVersionEntity.getId()))).thenReturn(java.util.Optional.of(draftVersionEntity));
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(Page.empty());

        RefBookVersion draftVersion = ModelGenerator.versionModel(draftVersionEntity);
        when(versionService.getById(eq(draftVersionEntity.getId()))).thenReturn(draftVersion);
        when(versionNumberStrategy.next(eq(REFBOOK_ID))).thenReturn("1.1");
        when(versionNumberStrategy.check(eq("1.1"), eq(REFBOOK_ID))).thenReturn(false);

        Integer draftId = 0;

        doThrow(new NotFoundException(new Message(VersionValidationImpl.DRAFT_NOT_FOUND, draftId)))
                .when(versionValidation).validateDraft(eq(draftId));
        when(versionRepository.exists(hasVersionId(draftVersionEntity.getId()).and(isDraft()))).thenReturn(true);

        //invalid draftId
        try {
            publishService.publish(draftId, "1.0", now, null);
            fail();
        } catch (UserException e) {
            Assert.assertEquals("draft.not.found", e.getCode());
            Assert.assertEquals(draftId, e.getArgs()[0]);
        }

        //invalid versionName
        when(versionRepository.exists(eq(isVersionOfRefBook(REFBOOK_ID)))).thenReturn(true);
        try {
            publishService.publish(draftVersionEntity.getId(), "1.1", now, null);
            fail();
        } catch (UserException e) {
            Assert.assertEquals("invalid.version.name", e.getCode());
            Assert.assertEquals("1.1", e.getArgs()[0]);
        }

        //invalid version period
        when(versionRepository.exists(eq(isVersionOfRefBook(REFBOOK_ID)))).thenReturn(true);
        try {
            publishService.publish(draftVersionEntity.getId(), null, now, LocalDateTime.MIN);
            fail();
        } catch (UserException e) {
            Assert.assertEquals("invalid.version.period", e.getCode());
        }

        //valid publishing, null version name
        publishService.publish(draftVersionEntity.getId(), null, now, null);
        assertEquals("1.1", draftVersionEntity.getVersion());

        verify(draftDataService).applyDraft(isNull(), eq(expectedDraftStorageCode), eq(now), any());
        verify(versionRepository).save(eq(expectedVersionEntity));
        verify(versionFileService, times(2)).save(eq(draftVersion), any(FileType.class), eq(null));
        //verify(fileStorage, times(2)).saveContent(any(InputStream.class), anyString());
        reset(versionRepository);
    }

    @Test
    public void testPublishNextVersionWithSameStructure() {

        RefBookVersionEntity versionEntity = createTestPublishedVersion();
        versionEntity.setVersion("2.1");

        RefBookVersionEntity draft = createTestDraftVersionEntity();
        draft.setStructure(versionEntity.getStructure());

        String expectedDraftStorageCode = draft.getStorageCode();

        RefBookVersionEntity expectedVersionEntity = createTestDraftVersionEntity();
        expectedVersionEntity.setVersion("2.2");
        expectedVersionEntity.setStatus(RefBookVersionStatus.PUBLISHED);
        expectedVersionEntity.setStorageCode(TEST_STORAGE_CODE);
        LocalDateTime now = LocalDateTime.now();
        expectedVersionEntity.setFromDate(now);

        when(versionRepository.findById(eq(draft.getId()))).thenReturn(java.util.Optional.of(draft));
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(anyString(), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(versionEntity);

        when(versionService.getById(eq(draft.getId())))
                .thenReturn(ModelGenerator.versionModel(draft));
        when(versionNumberStrategy.check("2.2", REFBOOK_ID)).thenReturn(true);
        when(versionRepository.exists(hasVersionId(draft.getId()).and(isDraft()))).thenReturn(true);

        publishService.publish(draft.getId(), expectedVersionEntity.getVersion(), now, null);

        verify(draftDataService)
                .applyDraft(eq(versionEntity.getStorageCode()), eq(expectedDraftStorageCode), eq(now), any());
        verify(versionRepository).save(eq(expectedVersionEntity));
        reset(versionRepository);
    }

    @Test
    public void testPublishWithAllOverlappingCases() {

        List<RefBookVersionEntity> actual = getVersionsForOverlappingPublish();
        List<RefBookVersionEntity> expected = getExpectedAfterOverlappingPublish();

        RefBookVersionEntity draftVersion = createTestDraftVersionEntity();

        when(versionRepository.findById(eq(draftVersion.getId()))).thenReturn(java.util.Optional.of(draftVersion));
        when(versionRepository.findAll(any(Predicate.class))).thenReturn(new PageImpl<>(actual));
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(new PageImpl<>(actual));
        when(versionService.getById(eq(draftVersion.getId())))
                .thenReturn(ModelGenerator.versionModel(draftVersion));
        when(versionNumberStrategy.check(eq("2.4"), eq(REFBOOK_ID))).thenReturn(true);
        doAnswer(invocation -> actual.removeIf(e -> e.getId().equals((invocation.getArguments()[0]))))
                .when(versionRepository).deleteById(anyInt());
        when(versionRepository.exists(eq(hasVersionId(draftVersion.getId()).and(isDraft())))).thenReturn(true);

        publishService.publish(draftVersion.getId(), "2.4", LocalDateTime.of(2017, 1, 4, 1, 1), LocalDateTime.of(2017, 1, 9, 1, 1));
        assertEquals(expected, actual);
        reset(versionRepository, versionService, versionNumberStrategy);

    }

    private List<RefBookVersionEntity> getVersionsForOverlappingPublish() {
        return new ArrayList<>(asList(
                createVersionEntity(REFBOOK_ID, 2, RefBookVersionStatus.PUBLISHED, LocalDateTime.of(2017, 1, 3, 1, 1), LocalDateTime.of(2017, 1, 5, 1, 1)),
                createVersionEntity(REFBOOK_ID, 3, RefBookVersionStatus.PUBLISHED, LocalDateTime.of(2017, 1, 6, 1, 1), LocalDateTime.of(2017, 1, 7, 1, 1)),
                createVersionEntity(REFBOOK_ID, 4, RefBookVersionStatus.PUBLISHED, LocalDateTime.of(2017, 1, 8, 1, 1), LocalDateTime.of(2017, 1, 10, 1, 1))
        ));
    }

    private List<RefBookVersionEntity> getExpectedAfterOverlappingPublish() {
        return Collections.singletonList(
                createVersionEntity(REFBOOK_ID, 2, RefBookVersionStatus.PUBLISHED, LocalDateTime.of(2017, 1, 3, 1, 1), LocalDateTime.of(2017, 1, 4, 1, 1))
        );
    }

    private RefBookVersionEntity createVersionEntity(Integer refBookId, Integer versionId, RefBookVersionStatus status,
                                                     LocalDateTime fromDate, LocalDateTime toDate) {
        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(refBookId);
        versionEntity.setRefBook(refBookEntity);
        versionEntity.setId(versionId);
        versionEntity.setStatus(status);
        versionEntity.setFromDate(fromDate);
        versionEntity.setToDate(toDate);
        return versionEntity;
    }

    private void setTestStructure(Structure structure) {
        structure.setAttributes(asList(
                Structure.Attribute.build("Kod", "Kod", FieldType.STRING, "Kod"),
                Structure.Attribute.build("Opis", "Opis", FieldType.STRING, "Opis"),
                Structure.Attribute.build("DATEBEG", "DATEBEG", FieldType.STRING, "DATEBEG"),
                Structure.Attribute.build("DATEEND", "DATEEND", FieldType.STRING, "DATEEND")
        ));
    }

    private RefBookVersionEntity createTestDraftVersionEntity() {
        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(1);
        entity.setStorageCode(TEST_DRAFT_CODE);
        entity.setRefBook(createTestRefBook());
        entity.setStatus(RefBookVersionStatus.DRAFT);
        entity.setStructure(new Structure());
        entity.setPassportValues(createTestPassportValues(entity));

        return entity;
    }

    private RefBookVersionEntity createTestPublishedVersion() {
        RefBookVersionEntity testDraftVersion = new RefBookVersionEntity();
        testDraftVersion.setId(3);
        testDraftVersion.setStorageCode("testVersionStorageCode");
        testDraftVersion.setRefBook(createTestRefBook());
        testDraftVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        testDraftVersion.setStructure(new Structure());
        testDraftVersion.setPassportValues(createTestPassportValues(testDraftVersion));
        return testDraftVersion;
    }

    private List<PassportValueEntity> createTestPassportValues(RefBookVersionEntity version) {
        List<PassportValueEntity> passportValues = new ArrayList<>();
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("fullName"), "full_name", version));
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("shortName"), "short_name", version));
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("annotation"), "annotation", version));
        return passportValues;
    }

    private RefBookEntity createTestRefBook() {
        RefBookEntity testRefBook = new RefBookEntity();
        testRefBook.setId(REFBOOK_ID);
        testRefBook.setCode(TEST_REF_BOOK);
        return testRefBook;
    }
}
