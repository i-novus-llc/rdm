package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateDataRequest;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FieldValueUtils;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.data.AfterUpdateDataStrategy;
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
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;
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
    private AfterUpdateDataStrategy afterUpdateDataStrategy;
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
    public void setUp() {

        reset(draftDataService);
        when(createDraftStorageStrategy.create(any(Structure.class))).thenReturn(NEW_DRAFT_CODE);

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategies());
        setField(draftService, "strategyLocator", strategyLocator);

        setField(structureChangeValidator, "draftDataService", draftDataService);
        setField(structureChangeValidator, "searchDataService", searchDataService);
        setField(structureChangeValidator, "versionRepository", versionRepository);

        setField(versionValidation, "refBookRepository", refBookRepository);
        setField(versionValidation, "versionRepository", versionRepository);
    }

    @Test
    public void testCreateWhenDraftWithSameStructure() {

        final RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID)))
                .thenReturn(draftEntity);
        when(versionRepository.saveAndFlush(any(RefBookVersionEntity.class))).thenReturn(draftEntity);

        final Draft expected = new Draft(DRAFT_ID, DRAFT_CODE, draftEntity.getOptLockValue());
        final Draft actual = draftService.create(new CreateDraftRequest(REFBOOK_ID, draftEntity.getStructure()));

        verify(deleteAllRowValuesStrategy).deleteAll(eq(draftEntity));
        assertEquals(expected, actual);
    }

    @Test
    public void testCreateWhenDraftWithDifferentStructure() {

        final RefBookVersionEntity draftEntity = createDraftEntity();
        draftEntity.getStructure().add(
                Structure.Attribute.build("temp_attr", "temp_attr", FieldType.STRING, null),
                null);

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(draftEntity);
        when(versionRepository.saveAndFlush(any(RefBookVersionEntity.class))).thenAnswer(v -> {

            final RefBookVersionEntity saved = (RefBookVersionEntity)(v.getArguments()[0]);
            saved.setId(draftEntity.getId() + 1);
            return saved;
        });

        final Structure structure = new Structure();
        structure.setAttributes(singletonList(Structure.Attribute.build("name", "name", FieldType.STRING, "description")));

        mockCreateDraftEntityStrategy(draftEntity.getRefBook(), structure);

        final Draft draftActual = draftService.create(new CreateDraftRequest(REFBOOK_ID, structure));
        assertNotEquals(draftEntity.getId(), draftActual.getId());
        assertNotEquals(DRAFT_CODE, draftActual.getStorageCode());
    }

    @Test
    public void testCreateWhenPublished() {

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(null);

        final RefBookVersionEntity publishedEntity = createPublishedEntity();
        when(versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(eq(REFBOOK_ID), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(publishedEntity);

        mockCreateDraftEntityStrategy(publishedEntity.getRefBook(), publishedEntity.getStructure());

        final RefBookVersionEntity savedDraftEntity = createDraftEntity();
        savedDraftEntity.setId(null);
        savedDraftEntity.setStorageCode(NEW_DRAFT_CODE);
        when(versionRepository.saveAndFlush(eq(savedDraftEntity))).thenReturn(savedDraftEntity);

        draftService.create(new CreateDraftRequest(REFBOOK_ID, new Structure()));

        verify(versionRepository).saveAndFlush(eq(savedDraftEntity));
    }

    @Test
    public void testCreateFromVersion() {

        // .createFromVersion
        final RefBookVersionEntity versionEntity = createPublishedEntity();
        when(versionRepository.findById(versionEntity.getId())).thenReturn(Optional.of(versionEntity));
        when(attributeValidationRepository.findAllByVersionId(versionEntity.getId())).thenReturn(emptyList());

        // .create
        final RefBookEntity refBookEntity = versionEntity.getRefBook();
        when(versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(refBookEntity.getId(), RefBookVersionStatus.PUBLISHED))
                .thenReturn(versionEntity);

        mockCreateDraftEntityStrategy(refBookEntity, versionEntity.getStructure());

        final ArgumentCaptor<RefBookVersionEntity> captor = ArgumentCaptor.forClass(RefBookVersionEntity.class);
        when(versionRepository.saveAndFlush(captor.capture())).thenReturn(new RefBookVersionEntity());

        final Draft draft = draftService.createFromVersion(versionEntity.getId());
        assertNotNull(draft);

        assertNotNull(captor);
        final RefBookVersionEntity draftEntity = captor.getValue();
        assertNotNull(draftEntity);
        assertNull(draftEntity.getId());
        assertEquals(NEW_DRAFT_CODE, draftEntity.getStorageCode());
        assertEquals(refBookEntity, draftEntity.getRefBook());
        assertEquals(versionEntity.getStructure(), draftEntity.getStructure());
    }

    @Test
    public void testCreateFromVersionWhenDraft() {

        final RefBookVersionEntity versionEntity = createDraftEntity();
        when(versionRepository.findById(versionEntity.getId())).thenReturn(Optional.of(versionEntity));

        final Draft draft = draftService.createFromVersion(versionEntity.getId());
        assertNotNull(draft);
        assertEquals(versionEntity.getId(), draft.getId());
        assertEquals(versionEntity.getStorageCode(), draft.getStorageCode());
        assertEquals(versionEntity.getOptLockValue(), draft.getOptLockValue());
    }

    private void mockCreateDraftEntityStrategy(RefBookEntity refBookEntity, Structure structure) {

        final RefBookVersionEntity createdEntity = createDraftEntity(refBookEntity);
        createdEntity.setId(null);
        createdEntity.setStructure(structure);
        createdEntity.setStorageCode(null);

        when(createDraftEntityStrategy.create(eq(refBookEntity), eq(structure), any()))
                .thenReturn(createdEntity);
    }

    @Test
    public void testRemove() {

        final RefBookVersionEntity draftEntity = createDraftEntity();
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

        final Structure.Attribute attribute = request.getAttribute();
        final Structure.Reference reference = request.getReference();
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

        final Structure.Attribute oldAttribute = oldStructure.getAttribute(oldCode);
        final Structure.Reference oldReference = oldStructure.getReference(oldCode);
        try {
            draftService.updateAttribute(draftId, request);
            fail(ERROR_WAITING + expectedExceptionClass.getSimpleName());

        } catch (Exception e) {
            assertEquals(expectedExceptionClass, e.getClass());
            assertEquals(message, getExceptionMessage(e));

            final Structure newStructure = versionService.getStructure(draftId);
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

        final Structure.Attribute attribute = oldStructure.getAttribute(attributeCode);
        final Structure.Reference reference = oldStructure.getReference(attributeCode);
        final DeleteAttributeRequest request = new DeleteAttributeRequest(null, attributeCode);
        try {
            draftService.deleteAttribute(draftId, request);
            fail(ERROR_WAITING + expectedExceptionClass.getSimpleName());

        } catch (Exception e) {
            assertEquals(expectedExceptionClass, e.getClass());
            assertEquals(message, getExceptionMessage(e));

            final Structure newStructure = versionService.getStructure(draftId);
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

        final FieldType[] primaryAllowedType = {
                FieldType.STRING,
                FieldType.INTEGER, FieldType.FLOAT,
                FieldType.REFERENCE,
                FieldType.DATE
        };

        final Serializable[] primaryValues = {
                "abc",
                BigInteger.valueOf(123L), BigDecimal.valueOf(123.123),
                new Reference("2", "-"),
                LocalDate.of(2019, 12, 12)
        };

        for (int i = 0; i < primaryAllowedType.length; i++) {

            testUpdateByPrimaryKey(primaryAllowedType[i], primaryValues[i]);
            Mockito.reset(versionService, versionRepository,
                    searchDataService, updateRowValuesStrategy, afterUpdateDataStrategy);
        }
    }

    private void testUpdateByPrimaryKey(FieldType primaryType, Serializable primaryValue) {

        final String primaryCode = "Primary";
        final String notPrimaryCode = "NotPrimary";
        final FieldType nonPrimaryType = FieldType.INTEGER;

        final Structure draftStructure = new Structure(
                List.of(
                        Structure.Attribute.buildPrimary(primaryCode, "-", primaryType, "-"),
                        Structure.Attribute.build(notPrimaryCode, "-", nonPrimaryType, "-")
                ),
                primaryType == FieldType.REFERENCE ? singletonList(new Structure.Reference(primaryCode, "REF_TO_CODE", "-")) : emptyList()
        );
        final RefBookVersionEntity draft = createDraftEntity();
        draft.setStructure(draftStructure);

        final long systemId = 123L;
        final int notPrimaryInitValue = 667;
        final int notPrimaryUpdatedValue = 668;

        final RefBookRowValue row = new RefBookRowValue();
        row.setSystemId(systemId);
        row.setFieldValues(List.of(
                FieldValueUtils.toFieldValue(primaryValue, primaryCode, primaryType),
                new IntegerFieldValue(notPrimaryCode, notPrimaryInitValue))
        );

        final PageImpl<RefBookRowValue> dataPage = new PageImpl<>(List.of(row));
        final DataPage<RowValue> pagedData = new DataPage<>(1, List.of(row), null);
        when(versionService.search(eq(draft.getId()), argThat(searchDataCriteria -> !searchDataCriteria.getAttributeFilters().isEmpty())))
                .thenReturn(dataPage);

        if (primaryType == FieldType.REFERENCE) {

            final Structure structure = new Structure(
                    singletonList(Structure.Attribute.buildPrimary("-", "-", FieldType.STRING, "-")),
                    emptyList()
            );

            final RefBookVersionEntity refToRefBookVersionEntity = new RefBookVersionEntity();
            refToRefBookVersionEntity.setId(1234567890);
            refToRefBookVersionEntity.setStructure(structure);
            when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq("REF_TO_CODE"), eq(RefBookVersionStatus.PUBLISHED)))
                    .thenReturn(refToRefBookVersionEntity);

            final RefBookVersion refToRefBookVersion = new RefBookVersion();
            refToRefBookVersion.setId(refToRefBookVersionEntity.getId());
            refToRefBookVersion.setCode("REF_TO_CODE");
            refToRefBookVersion.setStructure(structure);
            when(versionService.getLastPublishedVersion(eq("REF_TO_CODE"))).thenReturn(refToRefBookVersion);

            final PageImpl<RefBookRowValue> refToPage = new PageImpl<>(singletonList(
                    new RefBookRowValue(1L, singletonList(new StringFieldValue("-", "2")), null)
            ));
            when(versionService.search(eq(refToRefBookVersionEntity.getId()), any())).thenReturn(refToPage);
        }

        when(searchDataService.getPagedData(any())).thenReturn(pagedData);
        when(versionRepository.getOne(draft.getId())).thenReturn(draft);
        when(versionRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(searchDataService.findRows(anyString(), anyList(), anyList())).thenReturn(List.of(row));

        final Map<String, Object> map = new HashMap<>();
        map.put(primaryCode, primaryValue);
        map.put(notPrimaryCode, notPrimaryUpdatedValue);
        draftService.updateData(draft.getId(), new UpdateDataRequest(null, new Row(null, map)));

        verify(updateRowValuesStrategy).update(any(RefBookVersionEntity.class), any(), any());
        verify(afterUpdateDataStrategy).apply(any(RefBookVersionEntity.class), any(), any(), any());
    }

    private RefBookVersionEntity createDraftEntity() {

        return createDraftEntity(createRefBookEntity());
    }

    private RefBookVersionEntity createDraftEntity(RefBookEntity refBookEntity) {

        final RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setRefBook(refBookEntity);
        entity.setStructure(new Structure());
        entity.setStorageCode(DRAFT_CODE);
        entity.setStatus(RefBookVersionStatus.DRAFT);
        entity.setPassportValues(createPassportValues(entity));

        return entity;
    }

    private RefBookVersionEntity createPublishedEntity() {

        final RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(PUBLISHED_VERSION_ID);
        entity.setRefBook(createRefBookEntity());
        entity.setStructure(new Structure());
        entity.setStorageCode("testVersionStorageCode");
        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        entity.setPassportValues(createPassportValues(entity));

        return entity;
    }

    private RefBookEntity createRefBookEntity() {

        final RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFBOOK_ID);
        entity.setCode(REF_BOOK_CODE);

        return entity;
    }

    private List<PassportValueEntity> createPassportValues(RefBookVersionEntity version) {

        final List<PassportValueEntity> passportValues = new ArrayList<>();
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
            final UserException ue = (UserException) e;

            if (!isEmpty(ue.getMessages()))
                return ue.getMessages().get(0).getCode();
        }

        if (!StringUtils.isEmpty(e.getMessage()))
            return e.getMessage();

        return null;
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        final Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        final Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // Version + Draft:
        result.put(ValidateVersionNotArchivedStrategy.class, validateVersionNotArchivedStrategy);
        result.put(FindDraftEntityStrategy.class, findDraftEntityStrategy);
        result.put(CreateDraftEntityStrategy.class, createDraftEntityStrategy);
        result.put(CreateDraftStorageStrategy.class, createDraftStorageStrategy);
        // Data:
        result.put(UpdateRowValuesStrategy.class, updateRowValuesStrategy);
        result.put(AfterUpdateDataStrategy.class, afterUpdateDataStrategy);
        result.put(DeleteAllRowValuesStrategy.class, deleteAllRowValuesStrategy);

        return result;
    }
}
