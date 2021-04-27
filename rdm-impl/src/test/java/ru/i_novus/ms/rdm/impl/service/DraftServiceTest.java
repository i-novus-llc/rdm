package ru.i_novus.ms.rdm.impl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.platform.i18n.UserException;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.api.model.refdata.*;
import ru.i_novus.ms.rdm.api.model.version.*;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FieldValueUtils;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.MockFileStorage;
import ru.i_novus.ms.rdm.impl.file.UploadFileTestData;
import ru.i_novus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.draft.ValidateDraftExistsStrategy;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.model.version.UpdateValue.of;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;

@RunWith(MockitoJUnitRunner.class)
public class DraftServiceTest {

    private static final int REFBOOK_ID = 1;
    private static final String REF_BOOK_CODE = "test_refbook";

    private static final int DRAFT_ID = 2;
    private static final String DRAFT_CODE = "draft_code";
    private static final String NEW_DRAFT_CODE = "new_draft_code";
    private static final int PUBLISHED_VERSION_ID = 3;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
    private final FileStorage fileStorage = new MockFileStorage();
    @Mock
    private FileNameGenerator fileNameGenerator;

    @Mock
    private VersionValidationImpl versionValidation;
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
    private StructureChangeValidator structureChangeValidator;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private ValidateDraftExistsStrategy validateDraftExistsStrategy;
    @Mock
    private ValidateVersionNotArchivedStrategy validateVersionNotArchivedStrategy;

    private static final String UPD_SUFFIX = "_upd";
    private static final String PK_SUFFIX = "_pk";

    private static Structure.Attribute idAttribute;
    private static Structure.Attribute nameAttribute;
    private static Structure.Attribute updateIdAttribute;
    private static Structure.Attribute updateNameAttribute;
    private static Structure.Attribute codeAttribute;
    private static Structure.Attribute pkAttribute;

    private static Structure.Reference nameReference;
    private static Structure.Reference badNameReference;
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

        nameReference = new Structure.Reference(nameAttribute.getCode(), "REF_801", toPlaceholder(idAttribute.getCode()));
        badNameReference = new Structure.Reference("bad_" + nameAttribute.getCode(), "REF_801", toPlaceholder(idAttribute.getCode()));
        updateNameReference = new Structure.Reference(nameAttribute.getCode(), "REF_802", toPlaceholder(codeAttribute.getCode()));
        nullReference = new Structure.Reference(null, null, null);
    }

    @Before
    public void setUp() throws NoSuchFieldException {

        JsonUtil.jsonMapper = objectMapper;

        reset(draftDataService, fileNameGenerator, fileGeneratorFactory);
        when(draftDataService.createDraft(anyList())).thenReturn(NEW_DRAFT_CODE);

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategies());
        FieldSetter.setField(draftService, DraftServiceImpl.class.getDeclaredField("strategyLocator"), strategyLocator);

        FieldSetter.setField(structureChangeValidator, StructureChangeValidator.class.getDeclaredField("draftDataService"), draftDataService);
        FieldSetter.setField(structureChangeValidator, StructureChangeValidator.class.getDeclaredField("searchDataService"), searchDataService);
        FieldSetter.setField(structureChangeValidator, StructureChangeValidator.class.getDeclaredField("versionRepository"), versionRepository);

        FieldSetter.setField(versionValidation, VersionValidationImpl.class.getDeclaredField("refBookRepository"), refBookRepository);
        FieldSetter.setField(versionValidation, VersionValidationImpl.class.getDeclaredField("versionRepository"), versionRepository);
    }

    @Test
    public void testCreateWhenDraftWithSameStructure() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(draftEntity);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(draftEntity);

        Draft expected = new Draft(DRAFT_ID, DRAFT_CODE, draftEntity.getOptLockValue());
        Draft actual = draftService.create(new CreateDraftRequest(REFBOOK_ID, draftEntity.getStructure()));

        verify(draftDataService).deleteAllRows(eq(DRAFT_CODE));
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateWhenDraftWithDifferentStructure() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(draftEntity);
        when(versionRepository.save(any(RefBookVersionEntity.class))).thenAnswer(v -> {
            RefBookVersionEntity saved = (RefBookVersionEntity)(v.getArguments()[0]);
            saved.setId(draftEntity.getId() + 1);
            return saved;
        });

        Structure structure = new Structure();
        structure.setAttributes(singletonList(Structure.Attribute.build("name", "name", FieldType.STRING, "description")));

        Draft draftActual = draftService.create(new CreateDraftRequest(REFBOOK_ID, structure));

        assertNotEquals(draftEntity.getId(), draftActual.getId());
        assertNotEquals(DRAFT_CODE, draftActual.getStorageCode());
    }

    @Test
    public void testCreateWhenPublished() {

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(null);

        RefBookVersionEntity publishedEntity = createPublishedEntity();
        when(versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(eq(REFBOOK_ID), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(publishedEntity);

        RefBookVersionEntity draftEntity = createDraftEntity();
        draftEntity.setId(null);
        draftEntity.setStorageCode(NEW_DRAFT_CODE);
        when(versionRepository.save(eq(draftEntity))).thenReturn(draftEntity);

        draftService.create(new CreateDraftRequest(REFBOOK_ID, new Structure()));

        verify(versionRepository).save(eq(draftEntity));
    }

    @Test
    public void testCreateFromXlsWhenDraft() {

        RefBookVersionEntity draftEntity = createDraftEntityWithoutRefBookCode(REFBOOK_ID);
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(draftEntity);

        RefBookVersionEntity expectedRefBookVersion = createDraftEntityWithoutRefBookCode(draftEntity.getRefBook().getId());
        expectedRefBookVersion.setId(draftEntity.getId());
        expectedRefBookVersion.setStorageCode(NEW_DRAFT_CODE);

        Structure structure = new Structure();
        setTestStructure(structure);
        expectedRefBookVersion.setStructure(structure);

        draftService.create(draftEntity.getRefBook().getId(), createTestFileModel("/", "R002", "xlsx"));

        verify(versionRepository).save(eq(expectedRefBookVersion));
    }

    @Test
    public void testCreateFromXlsWhenPublished() {

        RefBookVersionEntity publishedEntity = createPublishedEntity();
        when(versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(eq(REFBOOK_ID), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(publishedEntity);

        RefBookVersionEntity draftEntity = createDraftEntityWithoutRefBookCode(REFBOOK_ID);
        draftEntity.setId(null);
        draftEntity.setStorageCode(NEW_DRAFT_CODE);

        Structure structure = new Structure();
        setTestStructure(structure);
        draftEntity.setStructure(structure);

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(null).thenReturn(draftEntity);

        draftService.create(REFBOOK_ID, createTestFileModel("/", "R002", "xlsx"));

        verify(versionRepository).save(eq(draftEntity));
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
    public void testCreateFromXmlWhenDraft() {

        RefBookVersionEntity firstDraftEntity = createDraftEntityWithoutRefBookCode(REFBOOK_ID);

        Integer draftId = 1;
        RefBookVersionEntity uploadedDraftEntity = createUploadedDraftEntity();
        uploadedDraftEntity.setId(draftId);

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID)))
                .thenReturn(createVersionCopy(firstDraftEntity))
                .thenReturn(createVersionCopy(uploadedDraftEntity));

        uploadedDraftEntity.setStorageCode(NEW_DRAFT_CODE);
        uploadedDraftEntity.setRefBook(firstDraftEntity.getRefBook());
        uploadedDraftEntity.setStructure(UploadFileTestData.createStructure()); // NB: reference

        RefBookVersionEntity referenceEntity = UploadFileTestData.createReferenceVersion();
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq(UploadFileTestData.REFERENCE_ENTITY_CODE), eq(RefBookVersionStatus.PUBLISHED))).thenReturn(referenceEntity);
        when(versionService.getLastPublishedVersion(eq(UploadFileTestData.REFERENCE_ENTITY_CODE))).thenReturn(ModelGenerator.versionModel(referenceEntity));

        PageImpl<RefBookRowValue> referenceRows = UploadFileTestData.createReferenceRows();
        when(versionService.search(eq(UploadFileTestData.REFERENCE_ENTITY_VERSION_ID), any(SearchDataCriteria.class))).thenReturn(referenceRows);

        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>(0, emptyList(), new Criteria()));

        when(versionRepository.save(any(RefBookVersionEntity.class))).thenReturn(uploadedDraftEntity);
        when(versionRepository.getOne(draftId)).thenReturn(uploadedDraftEntity);
        when(versionRepository.findById(draftId)).thenReturn(Optional.of(uploadedDraftEntity));

        ArgumentCaptor<RefBookVersionEntity> draftCaptor = ArgumentCaptor.forClass(RefBookVersionEntity.class);

        draftService.create(REFBOOK_ID, createTestFileModel("/file/", "uploadFile", "xml"));
        verify(versionRepository).save(draftCaptor.capture());

        uploadedDraftEntity.setStructure(UploadFileTestData.createStructure());
        RefBookVersionEntity actualDraftVersion = draftCaptor.getValue();
        actualDraftVersion.setId(uploadedDraftEntity.getId());

        Assert.assertEquals(uploadedDraftEntity, draftCaptor.getValue());

        // Old draft is not dropped since its structure is changed.
    }

    @Test
    public void testRemove() {

        RefBookVersionEntity draftEntity = createDraftEntityWithoutRefBookCode(REFBOOK_ID);
        when(versionRepository.findById(draftEntity.getId())).thenReturn(Optional.of(draftEntity));

        draftService.remove(draftEntity.getId());

        verify(versionRepository).deleteById(eq(draftEntity.getId()));
    }

    @Test
    public void testChangeStructure() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        final Integer draftId = draftEntity.getId();
        when(versionRepository.findById(eq(draftId))).thenReturn(Optional.of(draftEntity));
        when(versionService.getStructure(eq(draftId))).thenReturn(draftEntity.getStructure());

        doCallRealMethod().when(versionValidation).validateAttribute(any());
        doCallRealMethod().when(versionValidation).validateReferenceAbility(any());

        doCallRealMethod().when(structureChangeValidator).validateCreateAttribute(any(), any());
        doCallRealMethod().when(structureChangeValidator).validateUpdateAttribute(eq(draftId), any(), any());
        doCallRealMethod().when(structureChangeValidator).validateUpdateAttributeStorage(eq(draftId), any(), any(), any());

        // Добавление ссылочного атрибута
        RefBookEntity referredBook1 = new RefBookEntity();
        referredBook1.setCode("REF_801");
        RefBookVersionEntity referredEntity1 = new RefBookVersionEntity();
        referredEntity1.setRefBook(referredBook1);
        referredEntity1.setStructure(new Structure(singletonList(idAttribute), null));
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq(referredEntity1.getRefBook().getCode()), eq(RefBookVersionStatus.PUBLISHED))).thenReturn(referredEntity1);

        // -- Добавление атрибута null. Должна быть ошибка
        CreateAttributeRequest createRefAttribute = new CreateAttributeRequest(null, null, null);
        failCreateAttribute(draftId, createRefAttribute, "attribute.create.illegal.value", IllegalArgumentException.class);

        // -- Добавление ссылочного атрибута без ссылки. Должна быть ошибка
        createRefAttribute = new CreateAttributeRequest(null, nameAttribute, null);
        failCreateAttribute(draftId, createRefAttribute, "attribute.create.illegal.value", IllegalArgumentException.class);

        // -- Добавление обычного атрибута со ссылкой. Должна быть ошибка
        createRefAttribute = new CreateAttributeRequest(null, idAttribute, nameReference);
        failCreateAttribute(draftId, createRefAttribute, "attribute.create.illegal.value", IllegalArgumentException.class);

        // -- Добавление ссылочного атрибута с неверной привязкой к ссылке. Должна быть ошибка
        createRefAttribute = new CreateAttributeRequest(null, nameAttribute, badNameReference);
        failCreateAttribute(draftId, createRefAttribute, "attribute.create.illegal.reference.value", IllegalArgumentException.class);

        // -- Добавление атрибута с неверным кодом. Должна быть ошибка
        createRefAttribute = new CreateAttributeRequest(null, idAttribute, null);
        String idCode = idAttribute.getCode();
        idAttribute.setCode("Код");
        failCreateAttribute(draftId, createRefAttribute, "attribute.code.is.invalid", UserException.class);
        idAttribute.setCode(idCode);

        createRefAttribute = new CreateAttributeRequest(null, nameAttribute, nameReference);

        // -- Добавление атрибута с кодом null. Должна быть ошибка
        String nameCode = nameAttribute.getCode();
        nameAttribute.setCode(null);
        failCreateAttribute(draftId, createRefAttribute, "attribute.create.illegal.value", IllegalArgumentException.class);
        nameAttribute.setCode(nameCode);

        // -- Добавление атрибута с типом null. Должна быть ошибка
        FieldType nameType = nameAttribute.getType();
        nameAttribute.setType(null);
        failCreateAttribute(draftId, createRefAttribute, "attribute.create.illegal.value", IllegalArgumentException.class);
        nameAttribute.setType(nameType);

        // -- Добавление ссылочного атрибута - первичного ключа. Должна быть ошибка
        boolean isRefPrimary = nameAttribute.hasIsPrimary();
        nameAttribute.setIsPrimary(Boolean.TRUE);
        failCreateAttribute(draftId, createRefAttribute, "reference.attribute.cannot.be.primary.key", UserException.class);
        // -- Добавление ссылочного атрибута в структуру без первичного ключа. Должна быть ошибка
        nameAttribute.setIsPrimary(Boolean.FALSE);
        failCreateAttribute(draftId, createRefAttribute, "reference.book.must.have.primary.key", UserException.class);
        nameAttribute.setIsPrimary(isRefPrimary);

        // -- Добавление первичного ключа для возможности добавления ссылочного атрибута
        CreateAttributeRequest createIdAttribute = new CreateAttributeRequest(null, idAttribute, null);
        draftService.createAttribute(draftId, createIdAttribute);
        Structure structure = versionService.getStructure(draftId);
        assertTrue(structure.hasPrimary());
        assertEquals(createIdAttribute.getAttribute(), structure.getPrimaries().get(0));
        assertEquals(createIdAttribute.getReference(), structure.getReference(createIdAttribute.getAttribute().getCode()));

        // -- Добавление атрибута с существующим кодом. Должна быть ошибка
        failCreateAttribute(draftId, createIdAttribute, "attribute.with.code.already.exists", UserException.class);

        // -- Корректное добавление
        draftService.createAttribute(draftId, createRefAttribute);

        structure = versionService.getStructure(draftId);
        assertEquals(2, structure.getAttributes().size());
        assertEquals(nameAttribute, structure.getAttribute(nameAttribute.getCode()));
        assertEquals(nameReference, structure.getReference(nameAttribute.getCode()));

        // -- Удаление первичного ключа при наличии ссылки. Должна быть ошибка
        failDeleteAttribute(draftId, structure, createIdAttribute.getAttribute().getCode(), "reference.book.must.have.primary.key", UserException.class);

        // Изменение ссылочного атрибута
        RefBookEntity referredBook2 = new RefBookEntity();
        referredBook2.setCode("REF_802");
        RefBookVersionEntity referredEntity2 = new RefBookVersionEntity();
        referredEntity2.setRefBook(referredBook2);
        referredEntity2.setStructure(new Structure(singletonList(codeAttribute), null));
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq(referredEntity2.getRefBook().getCode()), eq(RefBookVersionStatus.PUBLISHED))).thenReturn(referredEntity2);

        // -- Корректное изменение
        when(draftDataService.isUnique(eq(DRAFT_CODE), anyList())).thenReturn(true);
        UpdateAttributeRequest updateRefAttribute = new UpdateAttributeRequest(null, updateNameAttribute, nameReference);
        draftService.updateAttribute(draftId, updateRefAttribute);
        assertEquals(updateNameAttribute, structure.getAttribute(updateRefAttribute.getCode()));
        assertEquals(nameReference, structure.getReference(updateRefAttribute.getCode()));

        // -- Изменение значений полей Reference
        updateRefAttribute = new UpdateAttributeRequest(null, updateNameAttribute, updateNameReference);
        draftService.updateAttribute(draftId, updateRefAttribute);
        assertEquals(updateNameReference, structure.getReference(updateRefAttribute.getCode()));

        // -- Передача null. Значение не должно измениться
        String updateRefCode = updateRefAttribute.getCode();
        updateRefAttribute.setReferenceCode(null);
        // -- Изменение поля на null. Значение должно обновиться
        updateRefAttribute.setDescription(null);
        draftService.updateAttribute(draftId, updateRefAttribute);
        assertEquals(updateNameReference.getReferenceCode(), structure.getReference(updateRefAttribute.getCode()).getReferenceCode());
        assertNull(structure.getAttribute(updateRefAttribute.getCode()).getDescription());

        // -- Изменение кода атрибута на null. Должна быть ошибка
        updateRefAttribute.setCode(null);
        failUpdateAttribute(draftId, updateRefAttribute, structure, updateRefCode, "attribute.update.illegal.value", IllegalArgumentException.class);
        updateRefAttribute.setCode(updateRefCode);

        // -- Изменение типа атрибута на null. Должна быть ошибка
        updateRefAttribute.setType(null);
        failUpdateAttribute(draftId, updateRefAttribute, structure, updateRefCode, "attribute.update.illegal.value", IllegalArgumentException.class);
        updateRefAttribute.setType(updateNameAttribute.getType());

        // -- Изменение кода ссылки на null. Должна быть ошибка (случай Reference -> Reference)
        updateRefAttribute.setReferenceCode(of(null));
        failUpdateAttribute(draftId, updateRefAttribute, structure, updateRefCode, "attribute.update.illegal.reference.value", IllegalArgumentException.class);

        // Изменение типа атрибута
        // -- Изменение со ссылочного на строковый. Ссылка должна удалиться из структуры
        updateNameAttribute.setType(FieldType.STRING);
        updateRefAttribute = new UpdateAttributeRequest(null, updateNameAttribute, nullReference);
        draftService.updateAttribute(draftId, updateRefAttribute);
        assertNull(structure.getReference(updateRefAttribute.getCode()));

        // -- Изменение со строкового на ссылочный, не заполнены поля для ссылки. Должна быть ошибка
        updateRefAttribute.setType(FieldType.REFERENCE);
        failUpdateAttribute(draftId, updateRefAttribute, structure, updateRefCode, "attribute.update.illegal.reference.value", IllegalArgumentException.class);

        // -- Изменение со ссылочного на строковый, все поля заполнены
        updateNameAttribute.setType(FieldType.REFERENCE);
        updateRefAttribute = new UpdateAttributeRequest(null, updateNameAttribute, updateNameReference);
        draftService.updateAttribute(draftId, updateRefAttribute);
        assertEquals(updateNameAttribute, structure.getAttribute(updateRefAttribute.getCode()));
        assertEquals(updateNameReference, structure.getReference(updateRefAttribute.getCode()));

        // -- Простановка первичности атрибута. Должна быть ошибка
        updateRefAttribute.setIsPrimary(of(Boolean.TRUE));
        failUpdateAttribute(draftId, updateRefAttribute, structure, updateRefCode, "reference.attribute.cannot.be.primary.key", UserException.class);
        updateRefAttribute.setIsPrimary(of(updateNameAttribute.getIsPrimary()));

        // -- Простановка выражения с несуществующим полем. Должна быть ошибка
        updateRefAttribute.setDisplayExpression(of(toPlaceholder("unknown")));
        failUpdateAttribute(draftId, updateRefAttribute, structure, updateRefCode, "reference.referred.attribute.not.found", NotFoundException.class);
        // -- Простановка выражения без полей
        updateRefAttribute.setDisplayExpression(of("text-only"));
        draftService.updateAttribute(draftId, updateRefAttribute);
        assertNotNull(structure.getReference(updateRefAttribute.getCode()));
        assertEquals("text-only", structure.getReference(updateRefAttribute.getCode()).getDisplayExpression());
        updateRefAttribute.setDisplayExpression(of(updateNameReference.getDisplayExpression()));
        draftService.updateAttribute(draftId, updateRefAttribute);

        // Изменение первичного атрибута
        UpdateAttributeRequest updatePrimaryAttribute = new UpdateAttributeRequest(null, updateIdAttribute, null);
        draftService.updateAttribute(draftId, updatePrimaryAttribute);
        assertEquals(updateIdAttribute, structure.getAttribute(updatePrimaryAttribute.getCode()));
        assertEquals(1, structure.getPrimaries().size());
        assertEquals(updateIdAttribute, structure.getPrimaries().get(0));

        // Добавление нового первичного атрибута. Первичность предыдущего атрибута должна быть удалена
        CreateAttributeRequest createPrimaryAttribute = new CreateAttributeRequest(null, pkAttribute, nullReference);
        draftService.createAttribute(draftId, createPrimaryAttribute);

        structure = versionService.getStructure(draftId);
        List<Structure.Attribute> primaries = structure.getPrimaries();
        assertEquals(1, primaries.size());
        assertTrue(primaries.contains(pkAttribute));
        assertFalse(primaries.contains(updateIdAttribute));

        // Удаление атрибута-ссылки для удаления первичного ключа
        DeleteAttributeRequest deleteAttributeRequest = new DeleteAttributeRequest(null, nameAttribute.getCode());
        draftService.deleteAttribute(draftId, deleteAttributeRequest);

        // Удаление первичности ключа. Не должно быть атрибутов - первичных ключей
        assertTrue(structure.hasPrimary());
        pkAttribute.setIsPrimary(false);
        updateRefAttribute = new UpdateAttributeRequest(null, pkAttribute, nullReference);
        draftService.updateAttribute(draftId, updateRefAttribute);
        structure = versionService.getStructure(draftId);
        assertFalse(structure.hasPrimary());
    }

    @Test
    public void testChangeStructureWithData() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        final Integer draftId = draftEntity.getId();

        String draftTable = draftEntity.getStorageCode();
        String draftTableWithData = draftTable + "_with_data";
        draftEntity.setStorageCode(draftTableWithData);

        when(versionRepository.findById(draftId)).thenReturn(Optional.of(draftEntity));
        when(versionService.getStructure(eq(draftId))).thenReturn(draftEntity.getStructure());

        when(searchDataService.hasData(eq(draftTableWithData))).thenReturn(true);

        doCallRealMethod().when(versionValidation).validateAttribute(any());

        doCallRealMethod().when(structureChangeValidator).validateCreateAttribute(any(), any());
        doCallRealMethod().when(structureChangeValidator).validateCreateAttributeStorage(any(), any(), eq(draftTableWithData));

        Structure.Attribute firstAttribute = Structure.Attribute.build("first", "Первый", FieldType.STRING, "описание first");
        CreateAttributeRequest createAttributeRequest = new CreateAttributeRequest(null, firstAttribute, null);
        draftService.createAttribute(draftId, createAttributeRequest);
        Structure structure = versionService.getStructure(draftId);
        assertFalse(structure.isEmpty());

        // -- Добавление первичного ключа при наличии данных. Должна быть ошибка
        Structure.Attribute secondAttribute = Structure.Attribute.buildPrimary("second", "Второй", FieldType.INTEGER, "описание second");
        createAttributeRequest = new CreateAttributeRequest(null, secondAttribute, null);
        failCreateAttribute(draftId, createAttributeRequest, "validation.required.pk.err", UserException.class);
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
        RefBookVersionEntity draft = createDraftEntity();
        draft.setStructure(draftStructure);

        long systemId = 123L;
        int notPrimaryInitValue = 667;
        int notPrimaryUpdatedValue = 668;
        RefBookRowValue row = new RefBookRowValue();
        row.setSystemId(systemId);
        row.setFieldValues(List.of(
                FieldValueUtils.toFieldValueByType(primaryValue, primaryCode, primaryType),
                new IntegerFieldValue(notPrimaryCode, notPrimaryInitValue))
        );

        PageImpl<RefBookRowValue> dataPage = new PageImpl<>(List.of(row));
        CollectionPage<RowValue> pagedData = new CollectionPage<>();
        pagedData.init(1, List.of(row));
        when(versionService.search(eq(draft.getId()), ArgumentMatchers.argThat(searchDataCriteria -> !searchDataCriteria.getAttributeFilters().isEmpty())))
                .thenReturn(dataPage);

        if (primaryType == FieldType.REFERENCE) {
            RefBookVersionEntity refToRefBookVersionEntity = new RefBookVersionEntity();
            refToRefBookVersionEntity.setId(1234567890);
            refToRefBookVersionEntity.setStructure(new Structure(singletonList(Structure.Attribute.buildPrimary("-", "-", FieldType.STRING, "-")), emptyList()));
            when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq("REF_TO_CODE"), eq(RefBookVersionStatus.PUBLISHED)))
                    .thenReturn(refToRefBookVersionEntity);
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
        when(versionRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(searchDataService.findRows(anyString(), anyList(), anyList())).thenReturn(List.of(row));

        Map<String, Object> map = new HashMap<>();
        map.put(primaryCode, primaryValue);
        map.put(notPrimaryCode, notPrimaryUpdatedValue);
        draftService.updateData(draft.getId(), new UpdateDataRequest(null, new Row(null, map)));

        verify(draftDataService, times(1)).updateRows(anyString(), any());
    }

    private void failCreateAttribute(Integer draftId,
                                     CreateAttributeRequest request,
                                     String message,
                                     Class expectedExceptionClass) {

        Structure.Attribute attribute = request.getAttribute();
        Structure.Reference reference = request.getReference();
        try {
            draftService.createAttribute(draftId, request);
            fail("Ожидается ошибка " + expectedExceptionClass.getSimpleName());

        } catch (Exception e) {
            assertEquals(expectedExceptionClass, e.getClass());
            assertEquals(message, getExceptionMessage(e));

            if (!"attribute.with.code.already.exists".equals(message)) {
                Structure newStructure = versionService.getStructure(draftId);
                assertNull("Атрибут не должен добавиться", attribute == null ? null : newStructure.getAttribute(attribute.getCode()));
                assertNull("Ссылка не должна добавиться", reference == null ? null : newStructure.getReference(reference.getAttribute()));
            }
        }
    }

    private void failUpdateAttribute(Integer draftId,
                                     UpdateAttributeRequest request,
                                     Structure oldStructure,
                                     String oldCode,
                                     String message,
                                     Class expectedExceptionClass) {

        Structure.Attribute oldAttribute = oldStructure.getAttribute(oldCode);
        Structure.Reference oldReference = oldStructure.getReference(oldCode);
        try {
            draftService.updateAttribute(draftId, request);
            fail("Ожидается ошибка " + expectedExceptionClass.getSimpleName());

        } catch (Exception e) {
            assertEquals(expectedExceptionClass, e.getClass());
            assertEquals(message, getExceptionMessage(e));

            Structure newStructure = versionService.getStructure(draftId);
            String newCode = request.getCode();
            if (StringUtils.isEmpty(newCode)) {
                assertNull("Не должно быть атрибута без кода", newStructure.getAttribute(newCode));
                assertNull("Не должно быть ссылки без кода атрибута", newStructure.getReference(newCode));

                newCode = oldCode;
            }

            assertEquals("Атрибут не должен измениться", oldAttribute, newStructure.getAttribute(newCode));
            assertEquals("Ссылка не должна измениться", oldReference, newStructure.getReference(newCode));
        }
    }

    private void failDeleteAttribute(Integer draftId,
                                     Structure oldStructure,
                                     String attributeCode,
                                     String message,
                                     Class expectedExceptionClass) {

        Structure.Attribute attribute = oldStructure.getAttribute(attributeCode);
        Structure.Reference reference = oldStructure.getReference(attributeCode);
        DeleteAttributeRequest request = new DeleteAttributeRequest(null, attributeCode);
        try {
            draftService.deleteAttribute(draftId, request);
            fail("Ожидается ошибка " + expectedExceptionClass.getSimpleName());

        } catch (Exception e) {
            assertEquals(expectedExceptionClass, e.getClass());
            assertEquals(message, getExceptionMessage(e));

            Structure newStructure = versionService.getStructure(draftId);
            assertNotNull("Атрибут не должен удалиться", newStructure.getAttribute(attribute.getCode()));
            if (reference != null) {
                assertNotNull("Ссылка не должна удалиться", newStructure.getReference(reference.getAttribute()));
            } else {
                assertNull("Ссылка не должна появиться", newStructure.getReference(attributeCode));
            }
        }
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

    private RefBookVersionEntity createVersionCopy(RefBookVersionEntity origin) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(origin.getId());
        entity.setRefBook(origin.getRefBook());
        entity.setStatus(origin.getStatus());
        entity.setPassportValues(new ArrayList<>(origin.getPassportValues()));
        entity.setStructure(origin.getStructure());
        entity.setStorageCode(origin.getStorageCode());

        return entity;
    }

    private RefBookVersionEntity createDraftEntity() {

        return createDraftEntity(createRefBookEntity());
    }

    private RefBookVersionEntity createDraftEntityWithoutRefBookCode(int refBookId) {

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(refBookId);

        return createDraftEntity(refBookEntity);
    }

    private RefBookVersionEntity createDraftEntity(RefBookEntity refBookEntity) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setRefBook(refBookEntity);
        entity.setStructure(new Structure());
        entity.setStorageCode(DRAFT_CODE);
        entity.setStatus(RefBookVersionStatus.DRAFT);
        entity.setPassportValues(createPassportValues(entity));

        return entity;
    }

    private RefBookVersionEntity createPublishedEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(PUBLISHED_VERSION_ID);
        entity.setRefBook(createRefBookEntity());
        entity.setStructure(new Structure());
        entity.setStorageCode("testVersionStorageCode");
        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        entity.setPassportValues(createPassportValues(entity));

        return entity;
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity entity = new RefBookEntity();
        entity.setId(REFBOOK_ID);
        entity.setCode(REF_BOOK_CODE);

        return entity;
    }

    private List<PassportValueEntity> createPassportValues(RefBookVersionEntity version) {

        List<PassportValueEntity> passportValues = new ArrayList<>();
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("fullName"), "full_name", version));
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("shortName"), "short_name", version));
        passportValues.add(new PassportValueEntity(new PassportAttributeEntity("annotation"), "annotation", version));
        return passportValues;
    }

    /*
     * Creates a version entity to be saved while creating a version from xml-file with passport values
     * */
    private RefBookVersionEntity createUploadedDraftEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(null);
        entity.setStructure(null);
        entity.setStatus(RefBookVersionStatus.DRAFT);
        entity.setPassportValues(UploadFileTestData.createPassportValues(entity));

        return entity;
    }

    /** Получение кода сообщения об ошибке из исключения. */
    private static String getExceptionMessage(Exception e) {

        if (e instanceof UserException) {
            UserException ue = (UserException) e;

            if (!isEmpty(ue.getMessages()))
                return ue.getMessages().get(0).getCode();
        }

        if (!StringUtils.isEmpty(e.getMessage()))
            return e.getMessage();

        return null;
    }

    private Map<RefBookType, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        Map<RefBookType, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookType.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        result.put(ValidateDraftExistsStrategy.class, validateDraftExistsStrategy);
        result.put(ValidateVersionNotArchivedStrategy.class, validateVersionNotArchivedStrategy);

        return result;
    }
}
