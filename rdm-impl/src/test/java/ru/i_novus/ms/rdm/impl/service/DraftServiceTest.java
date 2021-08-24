package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateDataRequest;
import ru.i_novus.ms.rdm.api.model.version.*;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FieldValueUtils;
import ru.i_novus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.data.DeleteAllRowValuesStrategy;
import ru.i_novus.ms.rdm.impl.strategy.data.UpdateRowValuesStrategy;
import ru.i_novus.ms.rdm.impl.strategy.draft.CreateDraftEntityStrategy;
import ru.i_novus.ms.rdm.impl.strategy.draft.CreateDraftStorageStrategy;
import ru.i_novus.ms.rdm.impl.strategy.draft.FindDraftEntityStrategy;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;

@RunWith(MockitoJUnitRunner.class)
public class DraftServiceTest {

    private static final String ERROR_WAITING = "Ожидается ошибка: ";

    private static final int REFBOOK_ID = 1;
    private static final String REF_BOOK_CODE = "test_refbook";

    private static final int DRAFT_ID = 2;
    private static final String DRAFT_CODE = "draft_code";
    private static final String NEW_DRAFT_CODE = "new_draft_code";
    private static final int PUBLISHED_VERSION_ID = 3;

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

    @Mock
    private VersionValidationImpl versionValidation;
    @Mock
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    @Mock
    private PassportValueRepository passportValueRepository;

    @Mock
    private AttributeValidationRepository attributeValidationRepository;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private StructureChangeValidator structureChangeValidator;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private ValidateVersionNotArchivedStrategy validateVersionNotArchivedStrategy;
    @Mock
    private FindDraftEntityStrategy findDraftEntityStrategy;
    @Mock
    private CreateDraftEntityStrategy createDraftEntityStrategy;
    @Mock
    private CreateDraftStorageStrategy createDraftStorageStrategy;
    @Mock
    private UpdateRowValuesStrategy updateRowValuesStrategy;
    @Mock
    private DeleteAllRowValuesStrategy deleteAllRowValuesStrategy;

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

        reset(draftDataService);
        when(createDraftStorageStrategy.create(any(Structure.class))).thenReturn(NEW_DRAFT_CODE);

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
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID)))
                .thenReturn(draftEntity);
        when(versionRepository.saveAndFlush(any(RefBookVersionEntity.class))).thenReturn(draftEntity);

        Draft expected = new Draft(DRAFT_ID, DRAFT_CODE, draftEntity.getOptLockValue());
        Draft actual = draftService.create(new CreateDraftRequest(REFBOOK_ID, draftEntity.getStructure()));

        verify(deleteAllRowValuesStrategy).deleteAll(eq(draftEntity));
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateWhenDraftWithDifferentStructure() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        draftEntity.getStructure().add(
                Structure.Attribute.build("temp_attr", "temp_attr", FieldType.STRING, null),
                null);

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(draftEntity);
        when(versionRepository.saveAndFlush(any(RefBookVersionEntity.class))).thenAnswer(v -> {
            RefBookVersionEntity saved = (RefBookVersionEntity)(v.getArguments()[0]);
            saved.setId(draftEntity.getId() + 1);
            return saved;
        });

        Structure structure = new Structure();
        structure.setAttributes(singletonList(Structure.Attribute.build("name", "name", FieldType.STRING, "description")));

        mockCreateDraftEntityStrategy(draftEntity.getRefBook(), structure);

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

        mockCreateDraftEntityStrategy(publishedEntity.getRefBook(), publishedEntity.getStructure());

        RefBookVersionEntity savedDraftEntity = createDraftEntity();
        savedDraftEntity.setId(null);
        savedDraftEntity.setStorageCode(NEW_DRAFT_CODE);
        when(versionRepository.saveAndFlush(eq(savedDraftEntity))).thenReturn(savedDraftEntity);

        draftService.create(new CreateDraftRequest(REFBOOK_ID, new Structure()));

        verify(versionRepository).saveAndFlush(eq(savedDraftEntity));
    }

    @Test
    public void testCreateFromVersion() {

        // .createFromVersion
        RefBookVersionEntity versionEntity = createPublishedEntity();
        when(versionRepository.findById(versionEntity.getId())).thenReturn(Optional.of(versionEntity));
        when(attributeValidationRepository.findAllByVersionId(versionEntity.getId())).thenReturn(emptyList());

        // .create
        RefBookEntity refBookEntity = versionEntity.getRefBook();
        when(versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(refBookEntity.getId(), RefBookVersionStatus.PUBLISHED))
                .thenReturn(versionEntity);

        mockCreateDraftEntityStrategy(refBookEntity, versionEntity.getStructure());

        ArgumentCaptor<RefBookVersionEntity> captor = ArgumentCaptor.forClass(RefBookVersionEntity.class);
        when(versionRepository.saveAndFlush(captor.capture())).thenReturn(new RefBookVersionEntity());

        Draft draft = draftService.createFromVersion(versionEntity.getId());
        assertNotNull(draft);

        assertNotNull(captor);
        RefBookVersionEntity draftEntity = captor.getValue();
        assertNotNull(draftEntity);
        assertNull(draftEntity.getId());
        assertEquals(NEW_DRAFT_CODE, draftEntity.getStorageCode());
        assertEquals(refBookEntity, draftEntity.getRefBook());
        assertEquals(versionEntity.getStructure(), draftEntity.getStructure());
    }

    @Test
    public void testCreateFromVersionWhenDraft() {

        RefBookVersionEntity versionEntity = createDraftEntity();
        when(versionRepository.findById(versionEntity.getId())).thenReturn(Optional.of(versionEntity));

        Draft draft = draftService.createFromVersion(versionEntity.getId());
        assertNotNull(draft);
        assertEquals(versionEntity.getId(), draft.getId());
        assertEquals(versionEntity.getStorageCode(), draft.getStorageCode());
        assertEquals(versionEntity.getOptLockValue(), draft.getOptLockValue());
    }

    private void mockCreateDraftEntityStrategy(RefBookEntity refBookEntity, Structure structure) {

        RefBookVersionEntity createdEntity = createDraftEntity(refBookEntity);
        createdEntity.setId(null);
        createdEntity.setStructure(structure);
        createdEntity.setStorageCode(null);

        when(createDraftEntityStrategy.create(eq(refBookEntity), eq(structure), any()))
                .thenReturn(createdEntity);
    }

    @Test
    public void testRemove() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findById(draftEntity.getId())).thenReturn(Optional.of(draftEntity));

        draftService.remove(draftEntity.getId());

        verify(versionRepository).deleteById(eq(draftEntity.getId()));
    }

    private void mockChangeStructure(RefBookVersionEntity draftEntity) {

        reset(versionRepository, versionService, versionValidation, structureChangeValidator);

        final Integer draftId = draftEntity.getId();
        when(versionRepository.findById(eq(draftId))).thenReturn(Optional.of(draftEntity));
        when(versionService.getStructure(eq(draftId))).thenReturn(draftEntity.getStructure());

        doCallRealMethod().when(versionValidation).validateAttribute(any());
        doCallRealMethod().when(versionValidation).validateReferenceAbility(any());

        doCallRealMethod().when(structureChangeValidator).validateCreateAttribute(any(), any());
        doCallRealMethod().when(structureChangeValidator).validateUpdateAttribute(eq(draftId), any(), any());
        doCallRealMethod().when(structureChangeValidator).validateUpdateAttributeStorage(eq(draftId), any(), any(), any());
    }

    private void failCreateAttribute(Integer draftId,
                                     CreateAttributeRequest request,
                                     String message,
                                     Class expectedExceptionClass) {

        Structure.Attribute attribute = request.getAttribute();
        Structure.Reference reference = request.getReference();
        try {
            draftService.createAttribute(draftId, request);
            fail(ERROR_WAITING + expectedExceptionClass.getSimpleName());

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
            fail(ERROR_WAITING + expectedExceptionClass.getSimpleName());

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
            fail(ERROR_WAITING + expectedExceptionClass.getSimpleName());

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

    @Test
    public void testUpdateVersionRowsByPrimaryKey() {

        FieldType[] primaryAllowedType = {
                FieldType.STRING,
                FieldType.INTEGER, FieldType.FLOAT,
                FieldType.REFERENCE,
                FieldType.DATE
        };
        Object[] primaryValues = {
                "abc",
                BigInteger.valueOf(123L), BigDecimal.valueOf(123.123),
                new Reference("2", "-"),
                LocalDate.of(2019, 12, 12)
        };

        for (int i = 0; i < primaryAllowedType.length; i++) {
            testUpdateByPrimaryKey(primaryAllowedType[i], primaryValues[i]);
            Mockito.reset(versionService, searchDataService, versionRepository, searchDataService, updateRowValuesStrategy);
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

        verify(updateRowValuesStrategy).update(any(RefBookVersionEntity.class), any(), any());
    }

    private RefBookVersionEntity createDraftEntity() {

        return createDraftEntity(createRefBookEntity());
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

        RefBookEntity entity = new DefaultRefBookEntity();
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

    private Structure.Attribute copy(Structure.Attribute attribute) {

        return Structure.Attribute.build(attribute);
    }

    private Structure.Reference copy(Structure.Reference reference) {

        return Structure.Reference.build(reference);
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

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // Version + Draft:
        result.put(ValidateVersionNotArchivedStrategy.class, validateVersionNotArchivedStrategy);
        result.put(FindDraftEntityStrategy.class, findDraftEntityStrategy);
        result.put(CreateDraftEntityStrategy.class, createDraftEntityStrategy);
        result.put(CreateDraftStorageStrategy.class, createDraftStorageStrategy);
        // Data:
        result.put(UpdateRowValuesStrategy.class, updateRowValuesStrategy);
        result.put(DeleteAllRowValuesStrategy.class, deleteAllRowValuesStrategy);

        return result;
    }
}
