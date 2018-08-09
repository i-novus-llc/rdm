package ru.inovus.ms.rdm.service;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
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
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.inovus.ms.rdm.model.UpdateValue.of;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.isPublished;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.isVersionOfRefBook;


@RunWith(MockitoJUnitRunner.class)
public class DraftServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";
    private static final String TEST_DRAFT_CODE = "test_draft_code";
    private static final String TEST_DRAFT_CODE_NEW = "test_draft_code_new";
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
        pkAttribute = Structure.Attribute.buildPrimary(nameAttribute.getCode() + PK_SUFFIX,nameAttribute.getName() + PK_SUFFIX, FieldType.STRING, nameAttribute.getDescription() + PK_SUFFIX);

        nameReference = new Structure.Reference(nameAttribute.getCode(), 801, codeAttribute.getCode(), emptyList(), emptyList());
        updateNameReference = new Structure.Reference(nameAttribute.getCode(), 802, codeAttribute.getCode(), singletonList(codeAttribute.getCode()), singletonList(codeAttribute.getCode()));
        nullReference = new Structure.Reference(null, null, null, null, null);
    }

    @Before
    public void setUp() throws Exception {
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
        LocalDateTime now = LocalDateTime.now();
        expectedVersionEntity.setFromDate(now);
        when(versionRepository.findOne(eq(testDraftVersion.getId()))).thenReturn(testDraftVersion);
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(null);
        draftService.publish(testDraftVersion.getId(), "1.0", now, null);

        verify(draftDataService).applyDraft(isNull(String.class), eq(expectedDraftStorageCode), eq(Date.from(now.atZone(ZoneId.systemDefault()).toInstant())));
        verify(versionRepository).save(eq(expectedVersionEntity));
        reset(versionRepository);
    }

    @Test
    public void testPublishOverlappingInLast() throws Exception {
        RefBookVersionEntity versionEntity = createTestPublishedVersion();
        RefBookVersionEntity draftVersion = createTestDraftVersion();
        LocalDateTime publishDate = LocalDateTime.now();

        when(versionRepository.findOne(eq(draftVersion.getId()))).thenReturn(draftVersion);
        when(versionRepository.findOne(any(BooleanExpression.class))).thenReturn(versionEntity);
        when(versionRepository.findAll(any(Predicate.class))).thenReturn(new PageImpl(singletonList(versionEntity)));
        try {
            draftService.publish(draftVersion.getId(), "1.0", publishDate, null);
            Assert.fail("publish overlapping version");
        } catch (UserException e) {
            Assert.assertEquals("overlapping.version.err", e.getCode());
        }

        reset(versionRepository);

    }

    @Test
    public void testPublishNextVersionWithSameStructure() throws Exception {

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


        draftService.publish(draft.getId(), expectedVersionEntity.getVersion(), now, null);

        verify(draftDataService).applyDraft(eq(versionEntity.getStorageCode()), eq(expectedDraftStorageCode), eq(Date.from(now.atZone(ZoneId.systemDefault()).toInstant())));
        verify(versionRepository).save(eq(expectedVersionEntity));
        reset(versionRepository);
    }

    @Test
    public void testPublishNextVersionWithSameStructureWithPeriodsOverlappingInFuture() throws Exception {

        LocalDateTime publishDate = LocalDateTime.now();

        RefBookVersionEntity overlappingVersionEntity = createTestPublishedVersion();
        overlappingVersionEntity.setFromDate(LocalDateTime.of(2017, 1, 1, 1, 1));
        RefBookVersionEntity expectedVersionEntity = createTestPublishedVersion();
        expectedVersionEntity.setToDate(publishDate);
        expectedVersionEntity.setFromDate(overlappingVersionEntity.getFromDate());
        RefBookVersionEntity draftVersion = createTestDraftVersion();

        when(versionRepository.findOne(eq(draftVersion.getId()))).thenReturn(draftVersion);
        when(versionRepository.findAll(any(Predicate.class))).thenReturn(new PageImpl(singletonList(overlappingVersionEntity)));

        draftService.publish(draftVersion.getId(), "2.4", publishDate, null);
        verify(versionRepository, times(1)).save(eq(expectedVersionEntity));

    }


    @Test
    public void testCreateWithExistingDraftSameStructure() throws Exception {
        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(testDraftVersion);
        Draft expected = new Draft(1, TEST_DRAFT_CODE);
        Draft actual = draftService.create(REFBOOK_ID, testDraftVersion.getStructure());

        verify(draftDataService).deleteAllRows(eq(TEST_DRAFT_CODE));
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateWithExistingDraftDifferentStructure() throws Exception {
        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        when(versionRepository.save(eq(testDraftVersion))).thenReturn(testDraftVersion);
        Structure structure = new Structure();
        structure.setAttributes(singletonList(Structure.Attribute.build("name", "name", FieldType.STRING, true, "description")));
        Draft draftActual = draftService.create(REFBOOK_ID, structure);

        assertEquals(testDraftVersion.getId(), draftActual.getId());
        assertNotEquals(TEST_DRAFT_CODE, draftActual.getStorageCode());
    }

    @Test
    public void testCreateWithoutDraftWithPublishedVersion() throws Exception {
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

        draftService.create(REFBOOK_ID, new Structure());

        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    @Test
    public void testCreateDraftFromFileWithDraft() {
        RefBookVersionEntity testDraftVersion = createTestDraftVersion();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        RefBookEntity refBook = new RefBookEntity();
        when(refBookRepository.findOne(REFBOOK_ID)).thenReturn(refBook);
        RefBookVersionEntity expectedRefBookVersion = createTestDraftVersion();
        expectedRefBookVersion.setId(null);
        expectedRefBookVersion.setStorageCode(TEST_DRAFT_CODE_NEW);
        expectedRefBookVersion.setRefBook(refBook);
        Structure structure = new Structure();
        setTestStructure(structure);
        expectedRefBookVersion.setStructure(structure);

        draftService.create(REFBOOK_ID, createTestFileModel());

        verify(dropDataService).drop(eq(Collections.singleton(TEST_DRAFT_CODE)));
        verify(versionRepository).delete(eq(testDraftVersion.getId()));
        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    private void setTestStructure(Structure structure) {
        structure.setAttributes(Arrays.asList(
                Structure.Attribute.build("Kod", "Kod", FieldType.STRING, false, "Kod"),
                Structure.Attribute.build("Opis", "Opis", FieldType.STRING, false, "Opis"),
                Structure.Attribute.build("DATEBEG", "DATEBEG", FieldType.STRING, false, "DATEBEG")
        ));
    }

    @Test
    public void testCreateDraftFromFileWithPublishedVersion() {
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

        draftService.create(REFBOOK_ID, createTestFileModel());

        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    @Test
    public void testRemoveDraft() {
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

        // добавление атрибута, получение структуры, проверка добавленного атрибута
        CreateAttribute createAttributeModel = new CreateAttribute(draftVersion.getId(), nameAttribute, nameReference);
        draftService.createAttribute(createAttributeModel);
        Structure structure = versionService.getStructure(draftVersion.getId());
        assertEquals(1, structure.getAttributes().size());
        assertEquals(nameAttribute, structure.getAttribute((nameAttribute.getCode())));
        assertEquals(nameReference, structure.getReference(nameAttribute.getCode()));

        // изменение атрибута и проверка
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
        assertEquals(pkAttribute, structure.getAttributes().stream().filter(Structure.Attribute::getIsPrimary).findFirst().orElse(null));

        // удаление первичности атрибута и проверка, что первичных нет
        assertTrue(structure.getAttributes().stream().anyMatch(Structure.Attribute::getIsPrimary));
        pkAttribute.setPrimary(false);
        updateAttributeModel = new UpdateAttribute(updateAttributeModel.getVersionId(), pkAttribute, nullReference);
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

    private FileModel createTestFileModel() {
        InputStream input = DraftServiceTest.class.getResourceAsStream("/R002.xlsx");
        String path = "R002";
        FileModel fileModel = new FileModel(path, "R002.xlsx");
        when(fileStorage.saveContent(eq(input), eq(path))).thenReturn(fileModel.generateFullPath());
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

    private Set<PassportValueEntity> createTestPassportValues(RefBookVersionEntity version){
        Set<PassportValueEntity> passportValues = new HashSet<>();
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("fullName"), "full_name", version));
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("shortName"), "short_name", version));
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("annotation"), "annotation", version));
        return passportValues;
    }

    private RefBookEntity createTestRefBook() {
        RefBookEntity testRefBook = new RefBookEntity();
        testRefBook.setId(REFBOOK_ID);
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
