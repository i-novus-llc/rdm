package ru.inovus.ms.rdm.service;

import com.querydsl.core.types.Predicate;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.AttributeValidationRepository;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.repositiory.VersionFileRepository;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.FileNameGenerator;
import ru.inovus.ms.rdm.util.ModelGenerator;
import ru.inovus.ms.rdm.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.util.VersionPeriodPublishValidation;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static ru.inovus.ms.rdm.model.UpdateValue.of;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;


@RunWith(MockitoJUnitRunner.class)
public class DraftServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";
    private static final String TEST_DRAFT_CODE = "test_draft_code";
    private static final String TEST_DRAFT_CODE_NEW = "test_draft_code_new";
    private static final String TEST_REF_BOOK = "test_ref_book";
    private static final int REFBOOK_ID = 2;

    @InjectMocks
    private DraftServiceImpl draftService;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private DraftDataService draftDataService;
    @Mock
    private VersionService versionService;
    @Mock
    private FieldFactory fieldFactory;
    @Mock
    private DropDataService dropDataService;
    @Mock
    private RefBookRepository refBookRepository;
    @Mock
    private SearchDataService searchDataService;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private FileNameGenerator fileNameGenerator;
    @Mock
    private VersionFileRepository versionFileRepository;
    @Mock
    private VersionNumberStrategy versionNumberStrategy;
    @Mock
    private VersionPeriodPublishValidation versionPeriodPublishValidation;
    @Mock
    private RefBookLockService refBookLockService;
    @Mock
    private AttributeValidationRepository attributeValidationRepository;

    private static final String UPD_SUFFIX = "_upd";
    private static final String PK_SUFFIX = "_pk";

    private static Structure.Attribute nameAttribute;
    private static Structure.Attribute updateNameAttribute;
    private static Structure.Attribute codeAttribute;
    private static Structure.Attribute pkAttribute;

    private static Structure.Reference nameReference;
    private static Structure.Reference updateNameReference;
    private static Structure.Reference nullReference;

    @BeforeClass
    public static void initialize() {

        nameAttribute = Structure.Attribute.buildPrimary("name", "Наименование", FieldType.REFERENCE, "описание");
        updateNameAttribute = Structure.Attribute.buildPrimary(nameAttribute.getCode(), nameAttribute.getName() + UPD_SUFFIX, FieldType.REFERENCE, nameAttribute.getDescription() + UPD_SUFFIX);
        codeAttribute = Structure.Attribute.buildPrimary("code", "Код", FieldType.STRING, "описание code");
        pkAttribute = Structure.Attribute.buildPrimary(nameAttribute.getCode() + PK_SUFFIX, nameAttribute.getName() + PK_SUFFIX, FieldType.STRING, nameAttribute.getDescription() + PK_SUFFIX);

        nameReference = new Structure.Reference(nameAttribute.getCode(), 801, codeAttribute.getCode(), null);
        updateNameReference = new Structure.Reference(nameAttribute.getCode(), 802, codeAttribute.getCode(), DisplayExpression.toPlaceholder(codeAttribute.getCode()));
        nullReference = new Structure.Reference(null, null, null, null);
    }

    @Before
    public void setUp() {
        when(draftDataService.applyDraft(any(), any(), any(), any())).thenReturn(TEST_STORAGE_CODE);
        when(draftDataService.createDraft(anyList())).thenReturn(TEST_DRAFT_CODE_NEW);
        when(fileNameGenerator.generateName(any(), eq(FileType.XLSX))).thenReturn("version.xlsx");
        when(fileNameGenerator.generateName(any(), eq(FileType.PDF))).thenReturn("version.pdf");
    }

    @Test
    public void testPublishFirstDraft() {

        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        String expectedDraftStorageCode = testDraftVersion.getStorageCode();
        RefBookVersionEntity expectedVersionEntity = createTestDraftVersion();
        expectedVersionEntity.setVersion("1.1");
        expectedVersionEntity.setStatus(RefBookVersionStatus.PUBLISHED);
        expectedVersionEntity.setStorageCode(TEST_STORAGE_CODE);
        LocalDateTime now = LocalDateTime.now();
        expectedVersionEntity.setFromDate(now);
        when(versionRepository.findOne(eq(testDraftVersion.getId()))).thenReturn(testDraftVersion);
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(null);
        when(versionService.getById(eq(testDraftVersion.getId())))
                .thenReturn(ModelGenerator.versionModel(testDraftVersion));
        when(versionNumberStrategy.next(eq(REFBOOK_ID))).thenReturn("1.1");
        when(versionNumberStrategy.check(eq("1.1"), eq(REFBOOK_ID))).thenReturn(false);
        when(versionRepository.exists(hasVersionId(testDraftVersion.getId()).and(isDraft()))).thenReturn(true);

        //invalid draftId
        try {
            draftService.publish(0, "1.0", now, null);
            fail();
        } catch (UserException e) {
            Assert.assertEquals("draft.not.found", e.getCode());
            Assert.assertEquals(0, e.getArgs()[0]);
        }

        //invalid versionName
        when(versionRepository.exists(eq(isVersionOfRefBook(REFBOOK_ID)))).thenReturn(true);
        try {
            draftService.publish(testDraftVersion.getId(), "1.1", now, null);
            fail();
        } catch (UserException e) {
            Assert.assertEquals("invalid.version.name", e.getCode());
            Assert.assertEquals("1.1", e.getArgs()[0]);
        }

        //invalid version period
        when(versionRepository.exists(eq(isVersionOfRefBook(REFBOOK_ID)))).thenReturn(true);
        try {
            draftService.publish(testDraftVersion.getId(), null, now, LocalDateTime.MIN);
            fail();
        } catch (UserException e) {
            Assert.assertEquals("invalid.version.period", e.getCode());
        }

        //valid publishing, null version name
        draftService.publish(testDraftVersion.getId(), null, now, null);
        assertEquals("1.1", testDraftVersion.getVersion());
        verify(draftDataService).applyDraft(isNull(String.class), eq(expectedDraftStorageCode), eq(Date.from(now.atZone(ZoneId.systemDefault()).toInstant())), any());
        verify(versionRepository).save(eq(expectedVersionEntity));
        verify(fileStorage, times(2)).saveContent(any(InputStream.class), anyString());
        reset(versionRepository);
    }

    @Test
    public void testPublishNextVersionWithSameStructure() {

        RefBookVersionEntity versionEntity = createTestPublishedVersion();
        versionEntity.setVersion("2.1");

        RefBookVersionEntity draft = createTestDraftVersion();
        draft.setStructure(versionEntity.getStructure());

        String expectedDraftStorageCode = draft.getStorageCode();

        RefBookVersionEntity expectedVersionEntity = createTestDraftVersion();
        expectedVersionEntity.setVersion("2.2");
        expectedVersionEntity.setStatus(RefBookVersionStatus.PUBLISHED);
        expectedVersionEntity.setStorageCode(TEST_STORAGE_CODE);
        LocalDateTime now = LocalDateTime.now();
        expectedVersionEntity.setFromDate(now);
        when(versionRepository.findOne(eq(draft.getId()))).thenReturn(draft);
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(new PageImpl(singletonList(versionEntity)));
        when(versionService.getById(eq(draft.getId())))
                .thenReturn(ModelGenerator.versionModel(draft));
        when(versionNumberStrategy.check("2.2", REFBOOK_ID)).thenReturn(true);
        when(versionRepository.exists(hasVersionId(draft.getId()).and(isDraft()))).thenReturn(true);

        draftService.publish(draft.getId(), expectedVersionEntity.getVersion(), now, null);

        verify(draftDataService)
                .applyDraft(eq(versionEntity.getStorageCode()), eq(expectedDraftStorageCode),
                        eq(Date.from(now.atZone(ZoneId.systemDefault()).toInstant())), any());
        verify(versionRepository).save(eq(expectedVersionEntity));
        reset(versionRepository);
    }

    @Test
    public void testPublishWithAllOverlappingCases() {

        List<RefBookVersionEntity> actual = getVersionsForOverlappingPublish();
        List<RefBookVersionEntity> expected = getExpectedAfterOverlappingPublish();

        RefBookVersionEntity draftVersion = createTestDraftVersion();

        when(versionRepository.findOne(eq(draftVersion.getId()))).thenReturn(draftVersion);
        when(versionRepository.findAll(any(Predicate.class))).thenReturn(new PageImpl(actual));
        when(versionService.getById(eq(draftVersion.getId())))
                .thenReturn(ModelGenerator.versionModel(draftVersion));
        when(versionNumberStrategy.check(eq("2.4"), eq(REFBOOK_ID))).thenReturn(true);
        doAnswer(invocation -> actual.removeIf(e -> e.getId().equals((invocation.getArguments()[0]))))
                .when(versionRepository).delete(anyInt());
        when(versionRepository.exists(eq(hasVersionId(draftVersion.getId()).and(isDraft())))).thenReturn(true);

        draftService.publish(draftVersion.getId(), "2.4", LocalDateTime.of(2017, 1, 4, 1, 1), LocalDateTime.of(2017, 1, 9, 1, 1));
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

    @Test
    public void testCreateWithExistingDraftSameStructure() {
        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(testDraftVersion);
        when(versionRepository.exists(isVersionOfRefBook(REFBOOK_ID))).thenReturn(true);
        Draft expected = new Draft(1, TEST_DRAFT_CODE);
        Draft actual = draftService.create(REFBOOK_ID, testDraftVersion.getStructure());

        verify(draftDataService).deleteAllRows(eq(TEST_DRAFT_CODE));
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateWithExistingDraftDifferentStructure() {
        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        when(versionRepository.save(eq(testDraftVersion))).thenReturn(testDraftVersion);
        when(versionRepository.exists(hasVersionId(testDraftVersion.getId()).and(isDraft()))).thenReturn(true);
        when(versionRepository.exists(eq(isVersionOfRefBook(REFBOOK_ID)))).thenReturn(true);
        Structure structure = new Structure();
        structure.setAttributes(singletonList(Structure.Attribute.build("name", "name", FieldType.STRING, "description")));
        Draft draftActual = draftService.create(REFBOOK_ID, structure);

        assertEquals(testDraftVersion.getId(), draftActual.getId());
        assertNotEquals(TEST_DRAFT_CODE, draftActual.getStorageCode());
    }

    @Test
    public void testCreateWithoutDraftWithPublishedVersion() {
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(null);
        RefBookVersionEntity lastRefBookVersion = createTestPublishedVersion();
        Page<RefBookVersionEntity> lastRefBookVersionPage = new PageImpl<>(Collections.singletonList(lastRefBookVersion));
        when(versionRepository
                .findAll(eq(isPublished().and(isVersionOfRefBook(REFBOOK_ID)))
                        , eq(new PageRequest(0, 1, new Sort(Sort.Direction.DESC, "fromDate"))))).thenReturn(lastRefBookVersionPage);
        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(REFBOOK_ID);
        when(refBookRepository.findOne(REFBOOK_ID)).thenReturn(refBook);
        RefBookVersionEntity expectedRefBookVersion = createTestDraftVersion();
        expectedRefBookVersion.setId(null);
        expectedRefBookVersion.setStorageCode(TEST_DRAFT_CODE_NEW);
        expectedRefBookVersion.setRefBook(refBook);
        when(versionRepository.save(eq(expectedRefBookVersion))).thenReturn(expectedRefBookVersion);
        when(versionRepository.exists(eq(isVersionOfRefBook(REFBOOK_ID)))).thenReturn(true);

        draftService.create(REFBOOK_ID, new Structure());

        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    @Test
    public void testCreateDraftFromXlsFileWithDraft() {
        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(REFBOOK_ID);

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        when(refBookRepository.findOne(REFBOOK_ID)).thenReturn(refBook);
        when(versionRepository.exists(hasVersionId(testDraftVersion.getId()).and(isDraft()))).thenReturn(true);
        when(versionRepository.exists(eq(isVersionOfRefBook(REFBOOK_ID)))).thenReturn(true);

        RefBookVersionEntity expectedRefBookVersion = createTestDraftVersion();
        expectedRefBookVersion.setId(null);
        expectedRefBookVersion.setStorageCode(TEST_DRAFT_CODE_NEW);
        expectedRefBookVersion.setRefBook(refBook);
        Structure structure = new Structure();
        setTestStructure(structure);
        expectedRefBookVersion.setStructure(structure);

        draftService.create(REFBOOK_ID, createTestFileModel("/", "R002", "xlsx"));

        verify(dropDataService).drop(eq(Collections.singleton(TEST_DRAFT_CODE)));
        verify(versionRepository).delete(eq(testDraftVersion.getId()));
        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    private void setTestStructure(Structure structure) {
        structure.setAttributes(asList(
                Structure.Attribute.build("Kod", "Kod", FieldType.STRING, "Kod"),
                Structure.Attribute.build("Opis", "Opis", FieldType.STRING, "Opis"),
                Structure.Attribute.build("DATEBEG", "DATEBEG", FieldType.STRING, "DATEBEG"),
                Structure.Attribute.build("DATEEND", "DATEEND", FieldType.STRING, "DATEEND")
        ));
    }

    @Test
    public void testCreateDraftFromXlsFileWithPublishedVersion() {
        RefBookVersionEntity lastRefBookVersion = createTestPublishedVersion();
        Page<RefBookVersionEntity> lastRefBookVersionPage = new PageImpl<>(singletonList(lastRefBookVersion));
        when(versionRepository
                .findAll(eq(isPublished().and(isVersionOfRefBook(REFBOOK_ID)))
                        , eq(new PageRequest(0, 1, new Sort(Sort.Direction.DESC, "fromDate"))))).thenReturn(lastRefBookVersionPage);
        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(REFBOOK_ID);
        when(refBookRepository.findOne(REFBOOK_ID)).thenReturn(refBook);
        RefBookVersionEntity expectedRefBookVersion = createTestDraftVersion();
        expectedRefBookVersion.setId(null);
        expectedRefBookVersion.setStorageCode(TEST_DRAFT_CODE_NEW);
        expectedRefBookVersion.setRefBook(refBook);
        Structure structure = new Structure();
        setTestStructure(structure);
        expectedRefBookVersion.setStructure(structure);
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(null).thenReturn(expectedRefBookVersion);
        when(versionRepository.exists(isVersionOfRefBook(REFBOOK_ID))).thenReturn(true);

        draftService.create(REFBOOK_ID, createTestFileModel("/", "R002", "xlsx"));

        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    @Test
    public void testCreateDraftFromXmlFileWithDraft() {
        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(REFBOOK_ID);

        RefBookVersionEntity versionBefore = createTestDraftVersion();
        versionBefore.setRefBook(refBook);

        RefBookVersionEntity versionWithPassport = createTestDraftVersionWithPassport();
        versionWithPassport.setId(null);
        versionWithPassport.setStorageCode(null);
        versionWithPassport.setRefBook(refBook);

        RefBookVersionEntity versionWithStructure = createTestDraftVersionWithPassport();
        versionWithStructure.setId(1);

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(createCopyOfVersion(versionBefore)).thenReturn(createCopyOfVersion(versionWithStructure));
        when(refBookRepository.findOne(REFBOOK_ID)).thenReturn(refBook);
        when(versionRepository.exists(hasVersionId(versionBefore.getId()).and(isDraft()))).thenReturn(true);
        when(versionRepository.exists(eq(isVersionOfRefBook(REFBOOK_ID)))).thenReturn(true);

        versionWithStructure.setStorageCode(TEST_DRAFT_CODE_NEW);
        versionWithStructure.setRefBook(refBook);
        versionWithStructure.setStructure(createFullTestStructure());

        ArgumentCaptor<RefBookVersionEntity> versionsCaptor = ArgumentCaptor.forClass(RefBookVersionEntity.class);
        draftService.create(REFBOOK_ID, createTestFileModel("/file/", "uploadFile", "xml"));

        verify(dropDataService).drop(eq(Collections.singleton(TEST_DRAFT_CODE)));
        verify(versionRepository).delete(eq(versionBefore.getId()));
        verify(versionRepository, times(2)).save(versionsCaptor.capture());

        assertVersions(asList(versionWithPassport, versionWithStructure), versionsCaptor.getAllValues());
    }

    @Test
    public void testRemoveDraft() {

        when(versionRepository.exists(hasVersionId(1).and(isDraft()))).thenReturn(true);

        draftService.remove(1);

        verify(versionRepository).delete(eq(1));
    }

    @Test
    public void testUpdateStructure() {
        RefBookVersionEntity draftVersion = createTestDraftVersion();
        RefBookEntity refBook = draftVersion.getRefBook();
        when(versionRepository.findOne(eq(draftVersion.getId()))).thenReturn(draftVersion);
        when(versionService.getStructure(eq(draftVersion.getId()))).thenReturn(draftVersion.getStructure());
        when(refBookRepository.findOne(eq(refBook.getId()))).thenReturn(refBook);
        when(versionRepository.exists(hasVersionId(draftVersion.getId()).and(isDraft()))).thenReturn(true);

        // добавление атрибута, получение структуры, проверка добавленного атрибута
        CreateAttribute createAttributeModel = new CreateAttribute(draftVersion.getId(), nameAttribute, nameReference);
        draftService.createAttribute(createAttributeModel);
        Structure structure = versionService.getStructure(draftVersion.getId());
        assertEquals(1, structure.getAttributes().size());
        assertEquals(nameAttribute, structure.getAttribute((nameAttribute.getCode())));
        assertEquals(nameReference, structure.getReference(nameAttribute.getCode()));

        // изменение атрибута и проверка
        when(draftDataService.isUnique(eq(TEST_DRAFT_CODE), anyList())).thenReturn(true);
        UpdateAttribute updateAttributeModel = new UpdateAttribute(draftVersion.getId(), updateNameAttribute, nameReference);
        draftService.updateAttribute(updateAttributeModel);
        assertEquals(updateNameAttribute, structure.getAttribute(updateAttributeModel.getCode()));
        assertEquals(nameReference, structure.getReference(updateAttributeModel.getCode()));

        // изменение referenceVersion, displayAttributes и sortingAttributes на новые значения у атрибута Reference и проверка
        updateAttributeModel = new UpdateAttribute(updateAttributeModel.getVersionId(), updateNameAttribute, updateNameReference);
        draftService.updateAttribute(updateAttributeModel);
        assertEquals(updateNameReference, structure.getReference(updateAttributeModel.getCode()));

        // новое значение не передается, проверка, что значение не изменилось
        updateAttributeModel.setReferenceVersion(null);
        // изменение некоторого поля атрибута на null и проверка, что значение обновилось
        updateAttributeModel.setDescription(of(null));
        draftService.updateAttribute(updateAttributeModel);
        assertEquals(updateNameReference.getReferenceVersion(), structure.getReference(updateAttributeModel.getCode()).getReferenceVersion());
        assertNull(structure.getAttribute(updateAttributeModel.getCode()).getDescription());

        // изменение кода атрибута на null, должна быть ошибка IllegalArgumentException
        updateAttributeModel.setCode(null);
        testUpdateWithExceptionExpected(updateAttributeModel, structure.getAttribute(updateAttributeModel.getCode()), structure.getReference(updateAttributeModel.getCode()));

        // изменение версии ссылки на null, должна быть ошибка IllegalArgumentException (случай Reference -> Reference)
        updateAttributeModel.setReferenceVersion(of(null));
        testUpdateWithExceptionExpected(updateAttributeModel, structure.getAttribute(updateAttributeModel.getCode()), structure.getReference(updateAttributeModel.getCode()));

        // изменение типа атрибута Reference -> String и проверка, что ссылка удалилась из структуры
        updateNameAttribute.setType(FieldType.STRING);
        updateAttributeModel = new UpdateAttribute(updateAttributeModel.getVersionId(), updateNameAttribute, nullReference);
        draftService.updateAttribute(updateAttributeModel);
        assertNull(structure.getReference(updateAttributeModel.getCode()));

        // изменение типа поля String -> Reference. Не все поля заполнены, ожидается ошибка
        updateAttributeModel.setType(FieldType.REFERENCE);
        testUpdateWithExceptionExpected(updateAttributeModel, structure.getAttribute(updateAttributeModel.getCode()), structure.getReference(updateAttributeModel.getCode()));

        // изменение типа поля String -> Reference. Все поля заполнены
        updateNameAttribute.setType(FieldType.REFERENCE);
        updateAttributeModel = new UpdateAttribute(updateAttributeModel.getVersionId(), updateNameAttribute, updateNameReference);
        draftService.updateAttribute(updateAttributeModel);
        assertEquals(updateNameAttribute, structure.getAttribute(updateAttributeModel.getCode()));
        assertEquals(updateNameReference, structure.getReference(updateAttributeModel.getCode()));

        // добавление нового первичного атрибута и проверка
        assertTrue(structure.getAttributes().stream().anyMatch(Structure.Attribute::getIsPrimary));
        assertEquals(updateNameAttribute, structure.getAttributes().stream().filter(Structure.Attribute::getIsPrimary).findFirst().orElse(null));
        CreateAttribute primaryCreateAttributeModel = new CreateAttribute(draftVersion.getId(), pkAttribute, nullReference);
        draftService.createAttribute(primaryCreateAttributeModel);
        structure = versionService.getStructure(draftVersion.getId());
        Set<Structure.Attribute> pks = structure.getAttributes().stream().filter(Structure.Attribute::getIsPrimary).collect(Collectors.toSet());
        assertEquals(2, pks.size());
        assertTrue(pks.contains(pkAttribute));
        assertTrue(pks.contains(updateNameAttribute));

        // удаление первичности атрибута и проверка
        assertTrue(structure.getAttributes().stream().anyMatch(Structure.Attribute::getIsPrimary));
        pkAttribute.setPrimary(false);
        updateAttributeModel = new UpdateAttribute(updateAttributeModel.getVersionId(), pkAttribute, nullReference);
        draftService.updateAttribute(updateAttributeModel);
        structure = versionService.getStructure(draftVersion.getId());
        pks = structure.getAttributes().stream().filter(Structure.Attribute::getIsPrimary).collect(Collectors.toSet());
        assertEquals(1, pks.size());
        assertTrue(pks.contains(updateNameAttribute));

        // удаление первичности второго атрибута и проверка, что первичных нет
        assertTrue(structure.getAttributes().stream().anyMatch(Structure.Attribute::getIsPrimary));
        updateNameAttribute.setPrimary(false);
        updateAttributeModel = new UpdateAttribute(updateAttributeModel.getVersionId(), updateNameAttribute, nullReference);
        draftService.updateAttribute(updateAttributeModel);
        structure = versionService.getStructure(draftVersion.getId());
        assertFalse(structure.getAttributes().stream().anyMatch(Structure.Attribute::getIsPrimary));
    }

    private void testUpdateWithExceptionExpected(UpdateAttribute updateAttribute, Structure.Attribute oldAttribute, Structure.Reference oldReference) {
        try {
            draftService.updateAttribute(updateAttribute);
            fail("Ожидается ошибка IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            Structure structure = versionService.getStructure(updateAttribute.getVersionId());
            assertEquals("Атрибут не должен измениться", oldAttribute, structure.getAttribute(updateAttribute.getCode()));
            assertEquals("Ссылка не должна измениться", oldReference, structure.getReference(updateAttribute.getCode()));
        }
    }

    private RefBookVersionEntity createTestDraftVersion() {
        RefBookVersionEntity testDraftVersion = new RefBookVersionEntity();
        testDraftVersion.setId(1);
        testDraftVersion.setStorageCode(TEST_DRAFT_CODE);
        testDraftVersion.setRefBook(createTestRefBook());
        testDraftVersion.setStatus(RefBookVersionStatus.DRAFT);
        testDraftVersion.setStructure(new Structure());
        testDraftVersion.setPassportValues(createTestPassportValues(testDraftVersion));
        return testDraftVersion;
    }

    /*
     * Example:
     * path = '/file/'
     * fileName = 'uploadFile'
     * extension = 'xml'
     **/
    private FileModel createTestFileModel(String path, String fileName, String extension) {
        InputStream input = DraftServiceTest.class.getResourceAsStream(path + fileName + "." + extension);
        FileModel fileModel = new FileModel(fileName, fileName + "." + extension);
        when(fileStorage.saveContent(eq(input), eq(fileName))).thenReturn(fileModel.generateFullPath());
        when(fileStorage.getContent(eq(fileModel.generateFullPath()))).thenReturn(input);
        String fullPath = fileStorage.saveContent(input, fileModel.getPath());
        fileModel.setPath(fullPath);
        return fileModel;
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

    private RefBookVersionEntity createCopyOfVersion(RefBookVersionEntity version) {
        RefBookVersionEntity copy = new RefBookVersionEntity();
        copy.setId(version.getId());
        copy.setStructure(version.getStructure());
        copy.setStatus(version.getStatus());
        copy.setRefBook(version.getRefBook());
        copy.setPassportValues(new ArrayList<>(version.getPassportValues()));
        copy.setStorageCode(version.getStorageCode());
        return copy;
    }

    /*
     * Creates a version entity to be saved while creating a version from xml-file with passport values
     * */
    private RefBookVersionEntity createTestDraftVersionWithPassport() {
        RefBookVersionEntity version = new RefBookVersionEntity();
        version.setId(null);
        version.setStatus(RefBookVersionStatus.DRAFT);
        version.setStructure(null);
        version.setPassportValues(asList(
                new PassportValueEntity(new PassportAttributeEntity("name"), "наименование справочника", version),
                new PassportValueEntity(new PassportAttributeEntity("shortName"), "краткое наим-ие", version),
                new PassportValueEntity(new PassportAttributeEntity("description"), "описание", version)
        ));

        return version;
    }

    private void assertVersions(List<RefBookVersionEntity> expected, List<RefBookVersionEntity> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getId(), actual.get(i).getId());
            assertEquals(expected.get(i).getComment(), actual.get(i).getComment());
            assertTrue(expected.get(i).getStructure() != null
                    ? expected.get(i).getStructure().storageEquals(actual.get(i).getStructure())
                    : actual.get(i).getStructure() == null);
            assertEquals(expected.get(i).getRefBook(), actual.get(i).getRefBook());
            assertEquals(expected.get(i).getStatus(), actual.get(i).getStatus());
            assertEquals(expected.get(i).getPassportValues().size(), actual.get(i).getPassportValues().size());
        }
    }

    private Structure createFullTestStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.build("string", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build("integer", "integer", FieldType.STRING, "число"),
                        Structure.Attribute.build("date", "date", FieldType.STRING, "дата"),
                        Structure.Attribute.build("boolean", "boolean", FieldType.STRING, "булево"),
                        Structure.Attribute.build("float", "float", FieldType.STRING, "дробное"),
                        Structure.Attribute.build("reference", "reference", FieldType.STRING, "ссылка")
                ),
                null
        );
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
