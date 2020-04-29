package ru.inovus.ms.rdm.impl.service;

import com.querydsl.core.types.Predicate;
import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.version.CreateAttribute;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.model.version.UpdateAttribute;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.api.util.FieldValueUtils;
import ru.inovus.ms.rdm.api.util.FileNameGenerator;
import ru.inovus.ms.rdm.api.util.json.JsonUtil;
import ru.inovus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.impl.entity.PassportValueEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.impl.file.FileStorage;
import ru.inovus.ms.rdm.impl.file.MockFileStorage;
import ru.inovus.ms.rdm.impl.file.UploadFileTestData;
import ru.inovus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.inovus.ms.rdm.impl.repository.*;
import ru.inovus.ms.rdm.impl.util.ModelGenerator;
import ru.inovus.ms.rdm.impl.validation.AttributeUpdateValidator;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static ru.inovus.ms.rdm.api.model.version.UpdateValue.of;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.isPublished;
import static ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates.isVersionOfRefBook;

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
    private RefBookRepository refBookRepository;
    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private DraftDataService draftDataService;
    @Mock
    private DropDataService dropDataService;
    @Mock
    private SearchDataService searchDataService;

    @Mock
    private VersionService versionService;
    @Mock
    private RefBookLockService refBookLockService;

    @Spy
    private FileStorage fileStorage = new MockFileStorage();
    @Mock
    private FileNameGenerator fileNameGenerator;

    @Mock
    private VersionValidation versionValidation;
    @Mock
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    @Mock
    private PassportValueRepository passportValueRepository;

    @Mock
    private AttributeValidationRepository attributeValidationRepository;

    @Mock
    private PerRowFileGeneratorFactory fileGeneratorFactory;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AttributeUpdateValidator attributeUpdateValidator;

    @Mock
    private RefBookConflictRepository conflictRepository;

    private static final String UPD_SUFFIX = "_upd";
    private static final String PK_SUFFIX = "_pk";

    private static Structure.Attribute idAttribute;
    private static Structure.Attribute nameAttribute;
    private static Structure.Attribute updateIdAttribute;
    private static Structure.Attribute updateNameAttribute;
    private static Structure.Attribute codeAttribute;
    private static Structure.Attribute pkAttribute;

    private static Structure.Reference nameReference;
    private static Structure.Reference updateNameReference;
    private static Structure.Reference nullReference;

    @BeforeClass
    public static void initialize() {

        idAttribute = Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, "описание id");
        nameAttribute = Structure.Attribute.build("name", "Наименование", FieldType.REFERENCE, "описание name");
        updateIdAttribute = Structure.Attribute.buildPrimary(idAttribute.getCode(), idAttribute.getName() + UPD_SUFFIX, FieldType.INTEGER, idAttribute.getDescription() + UPD_SUFFIX);
        updateNameAttribute = Structure.Attribute.build(nameAttribute.getCode(), nameAttribute.getName() + UPD_SUFFIX, FieldType.REFERENCE, nameAttribute.getDescription() + UPD_SUFFIX);
        codeAttribute = Structure.Attribute.buildPrimary("code", "Код", FieldType.STRING, "описание code");
        pkAttribute = Structure.Attribute.buildPrimary(nameAttribute.getCode() + PK_SUFFIX, nameAttribute.getName() + PK_SUFFIX, FieldType.STRING, nameAttribute.getDescription() + PK_SUFFIX);

        nameReference = new Structure.Reference(nameAttribute.getCode(), "REF_801", "");
        updateNameReference = new Structure.Reference(nameAttribute.getCode(), "REF_802", DisplayExpression.toPlaceholder(codeAttribute.getCode()));
        nullReference = new Structure.Reference(null, null, null);
    }

    @Before
    public void setUp() throws NoSuchFieldException {
        reset(draftDataService, fileNameGenerator, fileGeneratorFactory);
        when(draftDataService.createDraft(anyList())).thenReturn(TEST_DRAFT_CODE_NEW);
        FieldSetter.setField(attributeUpdateValidator, AttributeUpdateValidator.class.getDeclaredField("draftDataService"), draftDataService);
        FieldSetter.setField(attributeUpdateValidator, AttributeUpdateValidator.class.getDeclaredField("searchDataService"), searchDataService);
        FieldSetter.setField(attributeUpdateValidator, AttributeUpdateValidator.class.getDeclaredField("versionRepository"), versionRepository);
    }

    @Test
    public void testCreateWithExistingDraftSameStructure() {
        RefBookVersionEntity testDraftVersion = createTestDraftVersionEntity();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(testDraftVersion);
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(Page.empty());

        Draft expected = new Draft(1, TEST_DRAFT_CODE);
        Draft actual = draftService.create(new CreateDraftRequest(REFBOOK_ID, testDraftVersion.getStructure()));

        verify(draftDataService).deleteAllRows(eq(TEST_DRAFT_CODE));
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateWithExistingDraftDifferentStructure() {
        RefBookVersionEntity testDraftVersion = createTestDraftVersionEntity();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenAnswer(v -> {
            RefBookVersionEntity saved = (RefBookVersionEntity)(v.getArguments()[0]);
            saved.setId(testDraftVersion.getId() + 1);
            return saved;
        });
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(Page.empty());

        Structure structure = new Structure();
        structure.setAttributes(singletonList(Structure.Attribute.build("name", "name", FieldType.STRING, "description")));

        Draft draftActual = draftService.create(new CreateDraftRequest(REFBOOK_ID, structure));

        assertNotEquals(testDraftVersion.getId(), draftActual.getId());
        assertNotEquals(TEST_DRAFT_CODE, draftActual.getStorageCode());
    }

    @Test
    public void testCreateWithoutDraftWithPublishedVersion() {
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(null);
        RefBookVersionEntity lastRefBookVersion = createTestPublishedVersion();
        Page<RefBookVersionEntity> lastRefBookVersionPage = new PageImpl<>(Collections.singletonList(lastRefBookVersion));
        when(versionRepository
                .findAll(eq(isPublished().and(isVersionOfRefBook(REFBOOK_ID))),
                        eq(PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "fromDate"))))).thenReturn(lastRefBookVersionPage);
        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(REFBOOK_ID);
        RefBookVersionEntity expectedRefBookVersion = createTestDraftVersionEntity();
        expectedRefBookVersion.setId(null);
        expectedRefBookVersion.setStorageCode(TEST_DRAFT_CODE_NEW);
        expectedRefBookVersion.setRefBook(refBook);
        expectedRefBookVersion.getRefBook().setCode(TEST_REF_BOOK);
        when(versionRepository.save(eq(expectedRefBookVersion))).thenReturn(expectedRefBookVersion);

        draftService.create(new CreateDraftRequest(REFBOOK_ID, new Structure()));

        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    @Test
    public void testCreateDraftFromXlsFileWithDraft() {
        RefBookVersionEntity testDraftVersion = createTestDraftVersionEntity();
        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(REFBOOK_ID);

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(testDraftVersion);
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(Page.empty());

        RefBookVersionEntity expectedRefBookVersion = createTestDraftVersionEntity();
        expectedRefBookVersion.setId(null);
        expectedRefBookVersion.setStorageCode(TEST_DRAFT_CODE_NEW);
        expectedRefBookVersion.setRefBook(refBook);
        Structure structure = new Structure();
        setTestStructure(structure);
        expectedRefBookVersion.setStructure(structure);

        draftService.create(REFBOOK_ID, createTestFileModel("/", "R002", "xlsx"));

        verify(dropDataService).drop(eq(Collections.singleton(TEST_DRAFT_CODE)));
        verify(versionRepository).deleteById(eq(testDraftVersion.getId()));
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
                .findAll(eq(isPublished().and(isVersionOfRefBook(REFBOOK_ID))),
                        eq(PageRequest.of(0, 1, new Sort(Sort.Direction.DESC, "fromDate")))))
                .thenReturn(lastRefBookVersionPage);
        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(REFBOOK_ID);
        RefBookVersionEntity expectedRefBookVersion = createTestDraftVersionEntity();
        expectedRefBookVersion.setId(null);
        expectedRefBookVersion.setStorageCode(TEST_DRAFT_CODE_NEW);
        expectedRefBookVersion.setRefBook(refBook);
        Structure structure = new Structure();
        setTestStructure(structure);
        expectedRefBookVersion.setStructure(structure);
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(null).thenReturn(expectedRefBookVersion);

        draftService.create(REFBOOK_ID, createTestFileModel("/", "R002", "xlsx"));

        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    @Test
    public void testCreateDraftFromXmlFileWithDraft() {
        RefBookEntity refBook = new RefBookEntity();
        refBook.setId(REFBOOK_ID);

        RefBookVersionEntity versionBefore = createTestDraftVersionEntity();
        versionBefore.setRefBook(refBook);

        Integer draftId = 1;
        RefBookVersionEntity versionWithStructure = createTestDraftVersionWithPassport();
        versionWithStructure.setId(draftId);

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID)))
                .thenReturn(createCopyOfVersion(versionBefore)).thenReturn(createCopyOfVersion(versionWithStructure));
        when(versionRepository.findAll(any(Predicate.class), any(Pageable.class))).thenReturn(Page.empty());

        versionWithStructure.setStorageCode(TEST_DRAFT_CODE_NEW);
        versionWithStructure.setRefBook(refBook);
        versionWithStructure.setStructure(UploadFileTestData.createStructure()); // NB: reference

        RefBookVersionEntity referenceEntity = UploadFileTestData.createReferenceVersion();
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq(UploadFileTestData.REFERENCE_ENTITY_CODE), eq(RefBookVersionStatus.PUBLISHED))).thenReturn(referenceEntity);
        when(versionService.getLastPublishedVersion(eq(UploadFileTestData.REFERENCE_ENTITY_CODE))).thenReturn(ModelGenerator.versionModel(referenceEntity));

        PageImpl<RefBookRowValue> referenceRows = UploadFileTestData.createReferenceRows();
        when(versionService.search(eq(UploadFileTestData.REFERENCE_ENTITY_VERSION_ID), any(SearchDataCriteria.class))).thenReturn(referenceRows);

        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>(0, emptyList(), new Criteria()));

        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(versionWithStructure);
        when(versionRepository.getOne(draftId)).thenReturn(versionWithStructure);

        ArgumentCaptor<RefBookVersionEntity> draftCaptor = ArgumentCaptor.forClass(RefBookVersionEntity.class);

        draftService.create(REFBOOK_ID, createTestFileModel("/file/", "uploadFile", "xml"));
        verify(versionRepository).save(draftCaptor.capture());

        versionWithStructure.setStructure(UploadFileTestData.createStructure());
        RefBookVersionEntity actualDraftVersion = draftCaptor.getValue();
        actualDraftVersion.setId(versionWithStructure.getId());

        Assert.assertEquals(versionWithStructure, draftCaptor.getValue());

        // Old draft is not dropped since its structure is changed.
    }

    @Test
    public void testRemoveDraft() {

        draftService.remove(1);

        verify(versionRepository).deleteById(eq(1));
    }

    @Test
    public void testUpdateStructure() {

        RefBookVersionEntity draftVersion = createTestDraftVersionEntity();
        when(versionRepository.getOne(eq(draftVersion.getId()))).thenReturn(draftVersion);
        when(versionService.getStructure(eq(draftVersion.getId()))).thenReturn(draftVersion.getStructure());
        doCallRealMethod().when(attributeUpdateValidator).validateUpdateAttribute(any(), any());
        doCallRealMethod().when(attributeUpdateValidator).validateUpdateAttributeStorage(any(), any(), any());

        // добавление атрибута, получение структуры, проверка добавленного атрибута
//        RefBookVersion referredVersion1 = new RefBookVersion();
//        referredVersion1.setCode("REF_801");
//        referredVersion1.setStructure(new Structure(singletonList(codeAttribute), null));
//        when(versionService.getLastPublishedVersion(eq(referredVersion1.getCode()))).thenReturn(referredVersion1);

        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>());

        CreateAttribute createAttributeModel = new CreateAttribute(draftVersion.getId(), idAttribute, null);
        draftService.createAttribute(createAttributeModel);

        createAttributeModel = new CreateAttribute(draftVersion.getId(), nameAttribute, nameReference);
        draftService.createAttribute(createAttributeModel);
        Structure structure = versionService.getStructure(draftVersion.getId());
        assertEquals(2, structure.getAttributes().size());
        assertEquals(nameAttribute, structure.getAttribute((nameAttribute.getCode())));
        assertEquals(nameReference, structure.getReference(nameAttribute.getCode()));

        // изменение атрибута и проверка
        RefBookVersion referredVersion2 = new RefBookVersion();
        referredVersion2.setCode("REF_802");
        referredVersion2.setStructure(new Structure(singletonList(codeAttribute), null));
        when(versionService.getLastPublishedVersion(eq(referredVersion2.getCode()))).thenReturn(referredVersion2);

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
        updateAttributeModel.setReferenceCode(null);
        // изменение некоторого поля атрибута на null и проверка, что значение обновилось
        updateAttributeModel.setDescription(of(null));
        draftService.updateAttribute(updateAttributeModel);
        assertEquals(updateNameReference.getReferenceCode(), structure.getReference(updateAttributeModel.getCode()).getReferenceCode());
        assertNull(structure.getAttribute(updateAttributeModel.getCode()).getDescription());

        // изменение кода атрибута на null, должна быть ошибка IllegalArgumentException
        updateAttributeModel.setCode(null);
        testUpdateWithExceptionExpected(updateAttributeModel, structure.getAttribute(updateAttributeModel.getCode()), structure.getReference(updateAttributeModel.getCode()));

        // изменение версии ссылки на null, должна быть ошибка IllegalArgumentException (случай Reference -> Reference)
        updateAttributeModel.setReferenceCode(of(null));
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

        // добавление нового первичного атрибута и проверка, что первичность предыдущего удалена
        updateAttributeModel = new UpdateAttribute(updateAttributeModel.getVersionId(), updateIdAttribute, null);
        draftService.updateAttribute(updateAttributeModel);
        assertEquals(updateIdAttribute, structure.getAttribute(updateAttributeModel.getCode()));

        assertTrue(structure.getAttributes().stream().anyMatch(Structure.Attribute::hasIsPrimary));
        assertEquals(updateIdAttribute, structure.getAttributes().stream().filter(Structure.Attribute::hasIsPrimary).findFirst().orElse(null));

        CreateAttribute primaryCreateAttributeModel = new CreateAttribute(draftVersion.getId(), pkAttribute, nullReference);
        draftService.createAttribute(primaryCreateAttributeModel);

        structure = versionService.getStructure(draftVersion.getId());
        List<Structure.Attribute> primaries = structure.getPrimary();
        assertEquals(1, primaries.size());
        assertTrue(primaries.contains(pkAttribute));
        assertFalse(primaries.contains(updateIdAttribute));

        // удаление атрибута-ссылки для удаления первичности
        draftService.deleteAttribute(draftVersion.getId(), nameAttribute.getCode());

        // удаление первичности атрибута и проверка, что первичных нет
        assertTrue(structure.hasPrimary());
        pkAttribute.setPrimary(false);
        updateAttributeModel = new UpdateAttribute(updateAttributeModel.getVersionId(), pkAttribute, nullReference);
        draftService.updateAttribute(updateAttributeModel);
        structure = versionService.getStructure(draftVersion.getId());
        assertFalse(structure.hasPrimary());
    }

    @Test
    public void testUpdateVersionRowsByPrimaryKey() {
        FieldType[] primaryAllowedType = {FieldType.STRING, FieldType.INTEGER, FieldType.FLOAT, FieldType.REFERENCE, FieldType.DATE};
        Object[] primaryValues = {"abc", BigInteger.valueOf(123L), BigDecimal.valueOf(123.123), new Reference("2", "-"), LocalDate.of(2019, 12, 12)};
        for (int i = 0; i < primaryAllowedType.length; i++) {
            testUpdateByPrimaryKey(primaryAllowedType[i], primaryValues[i]);
            Mockito.reset(versionService, searchDataService, versionRepository, searchDataService, draftDataService);
        }
    }

    private void testUpdateByPrimaryKey(FieldType primaryType, Object primaryValue) {

        String primaryCode = "Primary";
        String notPrimaryCode = "NotPrimary";
        FieldType nonPrimaryType = FieldType.INTEGER;
        Structure draftStructure = new Structure(
                List.of(
                        Structure.Attribute.buildPrimary(primaryCode, "-", primaryType, "-"),
                        Structure.Attribute.build(notPrimaryCode, "-", nonPrimaryType, "-")
                ),
                primaryType == FieldType.REFERENCE ? singletonList(new Structure.Reference(primaryCode, "REF_TO_CODE", "-")) : emptyList()
        );
        RefBookVersionEntity draft = createTestDraftVersionEntity();
        draft.setStructure(draftStructure);

        long systemId = 123L;
        int notPrimaryInitValue = 667;
        int notPrimaryUpdatedValue = 668;
        RowValue<Long> row = new RefBookRowValue();
        row.setSystemId(systemId);
        row.setFieldValues(List.of(
                FieldValueUtils.getFieldValueFromFieldType(primaryValue, primaryCode, primaryType),
                new IntegerFieldValue(notPrimaryCode, notPrimaryInitValue))
        );

        PageImpl page = new PageImpl<>(List.of(row));
        CollectionPage pagedData = new CollectionPage();
        pagedData.init(1, List.of(row));
        when(versionService.search(eq(draft.getId()), ArgumentMatchers.argThat(searchDataCriteria -> !searchDataCriteria.getAttributeFilter().isEmpty()))).thenReturn(page);

        if (primaryType == FieldType.REFERENCE) {
            RefBookVersionEntity refToRefBookVersionEntity = new RefBookVersionEntity();
            refToRefBookVersionEntity.setId(1234567890);
            refToRefBookVersionEntity.setStructure(new Structure(singletonList(Structure.Attribute.buildPrimary("-", "-", FieldType.STRING, "-")), emptyList()));
            when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq("REF_TO_CODE"), eq(RefBookVersionStatus.PUBLISHED))).thenReturn(refToRefBookVersionEntity);
            RefBookVersion refToRefBookVersion = new RefBookVersion();
            refToRefBookVersion.setId(refToRefBookVersionEntity.getId());
            refToRefBookVersion.setCode("REF_TO_CODE");
            refToRefBookVersion.setStructure(refToRefBookVersionEntity.getStructure());
            when(versionService.getLastPublishedVersion(eq("REF_TO_CODE"))).thenReturn(refToRefBookVersion);

            PageImpl<RefBookRowValue> refToPage = new PageImpl<>(singletonList(
                    new RefBookRowValue(1L, singletonList(new StringFieldValue("-", "2")), null)
            ));
            when(versionService.search(eq(refToRefBookVersionEntity.getId()), any())).thenReturn(refToPage);
        }

        when(searchDataService.getPagedData(any())).thenReturn(pagedData);
        when(versionRepository.getOne(draft.getId())).thenReturn(draft);
        when(searchDataService.findRows(anyString(), anyList(), anyList())).thenReturn(List.of(row));

        Map<String, Object> map = new HashMap<>();
        map.put(primaryCode, primaryValue);
        map.put(notPrimaryCode, notPrimaryUpdatedValue);
        draftService.updateData(draft.getId(), new Row(null, map));

        verify(draftDataService, times(1)).updateRows(anyString(), any());
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

    /*
     * Example:
     * path = '/file/'
     * fileName = 'uploadFile'
     * extension = 'xml'
     **/
    private FileModel createTestFileModel(String path, String fileName, String extension) {
        String fullName = fileName + "." + extension;

        FileModel fileModel = new FileModel(fileName, fullName); // NB: fileName as path!
        if (fileStorage instanceof MockFileStorage) {
            ((MockFileStorage)fileStorage).setFileModel(fileModel);
            ((MockFileStorage)fileStorage).setFilePath(path + fullName);
        }

        InputStream input = DraftServiceTest.class.getResourceAsStream(path + fullName);

        if (!(fileStorage instanceof MockFileStorage)) {
            when(fileStorage.saveContent(eq(input), eq(fileName))).thenReturn(fileModel.generateFullPath());
            when(fileStorage.getContent(eq(fileModel.generateFullPath())))
                    .thenReturn(input,
                            DraftServiceTest.class.getResourceAsStream(path + fullName),
                            DraftServiceTest.class.getResourceAsStream(path + fullName),
                            DraftServiceTest.class.getResourceAsStream(path + fullName));
        }

        // NB: Check mock.
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
        version.setPassportValues(UploadFileTestData.createPassportValues(version));

        return version;
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
