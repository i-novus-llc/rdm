package ru.i_novus.ms.rdm.impl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateFromFileRequest;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.api.util.row.RowMapper;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;
import ru.i_novus.ms.rdm.impl.entity.*;
import ru.i_novus.ms.rdm.impl.file.UploadFileTestData;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.draft.*;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.*;

import java.io.InputStream;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.springframework.util.CollectionUtils.isEmpty;

@RunWith(MockitoJUnitRunner.class)
public class DraftServiceFileTest {

    private static final String ERROR_WAITING = "Ожидается ошибка: ";

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

    @Mock
    private VersionValidationImpl versionValidation;

    @Mock
    private PassportValueRepository passportValueRepository;

    @Mock
    private AttributeValidationRepository attributeValidationRepository;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private VersionFileServiceImpl versionFileService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private ValidateVersionNotArchivedStrategy validateVersionNotArchivedStrategy;
    @Mock
    private ValidateDraftExistsStrategy validateDraftExistsStrategy;
    @Mock
    private FindDraftEntityStrategy findDraftEntityStrategy;
    @Mock
    private CreateDraftEntityStrategy createDraftEntityStrategy;
    @Mock
    private CreateDraftStorageStrategy createDraftStorageStrategy;

    @Before
    public void setUp() throws NoSuchFieldException {

        JsonUtil.jsonMapper = objectMapper;

        reset(draftDataService);
        when(createDraftStorageStrategy.create(any(Structure.class))).thenReturn(NEW_DRAFT_CODE);

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategies());
        FieldSetter.setField(draftService, DraftServiceImpl.class.getDeclaredField("strategyLocator"), strategyLocator);

        FieldSetter.setField(versionValidation, VersionValidationImpl.class.getDeclaredField("refBookRepository"), refBookRepository);
        FieldSetter.setField(versionValidation, VersionValidationImpl.class.getDeclaredField("versionRepository"), versionRepository);
    }

    @Test
    public void testCreateFromXlsWhenDraft() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(draftEntity);

        RefBookEntity refBookEntity = draftEntity.getRefBook();
        RefBookVersionEntity createdEntity = createDraftEntity(refBookEntity);
        createdEntity.setId(draftEntity.getId());
        final Structure stringStructure = createStringStructure();
        createdEntity.setStructure(stringStructure);
        createdEntity.setStorageCode(NEW_DRAFT_CODE);

        when(findDraftEntityStrategy.find(refBookEntity)).thenReturn(createdEntity);
        when(createDraftEntityStrategy.create(refBookEntity, stringStructure, draftEntity.getPassportValues()))
                .thenReturn(createdEntity);

        when(versionRepository.saveAndFlush(eq(createdEntity))).thenReturn(createdEntity);

        final FileModel fileModel = createFileModel("/", "R002", "xlsx");
        doCallRealMethod()
                .when(versionFileService).processRows(eq(fileModel), any(RowsProcessor.class), any(RowMapper.class));

        draftService.create(refBookEntity.getId(), fileModel);

        verify(versionRepository).saveAndFlush(eq(createdEntity));
    }

    @Test
    public void testCreateFromXlsWhenPublished() {

        RefBookVersionEntity publishedEntity = createPublishedEntity();
        when(versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(eq(REFBOOK_ID), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(publishedEntity);
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID))).thenReturn(null);

        RefBookEntity refBookEntity = publishedEntity.getRefBook();
        RefBookVersionEntity createdEntity = createDraftEntity(refBookEntity);
        createdEntity.setId(null);
        createdEntity.setStructure(createStringStructure());
        createdEntity.setStorageCode(NEW_DRAFT_CODE);

        when(findDraftEntityStrategy.find(publishedEntity.getRefBook())).thenReturn(createdEntity);

        mockCreateDraftEntityStrategy(refBookEntity, createdEntity.getStructure());

        final FileModel fileModel = createFileModel("/", "R002", "xlsx");
        doCallRealMethod()
                .when(versionFileService).processRows(eq(fileModel), any(RowsProcessor.class), any(RowMapper.class));

        draftService.create(REFBOOK_ID, fileModel);

        verify(versionRepository).saveAndFlush(eq(createdEntity));
    }

    private Structure createStringStructure() {

        Structure structure = new Structure();
        structure.setAttributes(asList(
                Structure.Attribute.build("Kod", "Kod", FieldType.STRING, "Kod"),
                Structure.Attribute.build("Opis", "Opis", FieldType.STRING, "Opis"),
                Structure.Attribute.build("DATEBEG", "DATEBEG", FieldType.STRING, "DATEBEG"),
                Structure.Attribute.build("DATEEND", "DATEEND", FieldType.STRING, "DATEEND")
        ));
        return structure;
    }

    @Test
    public void testCreateFromXmlWhenDraft() {

        RefBookVersionEntity firstDraftEntity = createDraftEntity();

        Integer uploadedDraftId = DRAFT_ID + 1; // Только для различения сущности
        RefBookVersionEntity uploadedDraftEntity = createUploadedDraftEntity(firstDraftEntity.getRefBook());
        uploadedDraftEntity.setId(uploadedDraftId);

        // .create
        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(REFBOOK_ID)))
                .thenReturn(createVersionCopy(firstDraftEntity))
                .thenReturn(createVersionCopy(uploadedDraftEntity));

        uploadedDraftEntity.setStorageCode(NEW_DRAFT_CODE);
        uploadedDraftEntity.setRefBook(firstDraftEntity.getRefBook());
        uploadedDraftEntity.setStructure(UploadFileTestData.createStructure()); // NB: reference

        // .updateDataFromFile
        RefBookVersionEntity referenceEntity = UploadFileTestData.createReferenceVersion();
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq(UploadFileTestData.REFERENCE_ENTITY_CODE), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(referenceEntity);
        when(versionService.getLastPublishedVersion(eq(UploadFileTestData.REFERENCE_ENTITY_CODE)))
                .thenReturn(ModelGenerator.versionModel(referenceEntity));

        PageImpl<RefBookRowValue> referenceRows = UploadFileTestData.createReferenceRows();
        when(versionService.search(eq(UploadFileTestData.REFERENCE_ENTITY_VERSION_ID), any(SearchDataCriteria.class))).thenReturn(referenceRows);

        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>(0, emptyList(), new Criteria()));

        when(versionRepository.saveAndFlush(any(RefBookVersionEntity.class))).thenReturn(uploadedDraftEntity);
        when(versionRepository.getOne(uploadedDraftId)).thenReturn(uploadedDraftEntity);
        when(versionRepository.findById(uploadedDraftId)).thenReturn(Optional.of(uploadedDraftEntity));

        mockCreateDraftEntityStrategy(uploadedDraftEntity.getRefBook(), uploadedDraftEntity.getStructure());

        final FileModel fileModel = createFileModel("/file/", "uploadFile", "xml");
        doCallRealMethod()
                .when(versionFileService).processRows(eq(fileModel), any(RowsProcessor.class), any(RowMapper.class));

        draftService.create(REFBOOK_ID, fileModel);

        ArgumentCaptor<RefBookVersionEntity> draftCaptor = ArgumentCaptor.forClass(RefBookVersionEntity.class);
        verify(versionRepository).saveAndFlush(draftCaptor.capture());

        uploadedDraftEntity.setStructure(UploadFileTestData.createStructure());
        RefBookVersionEntity actualDraftVersion = draftCaptor.getValue();
        actualDraftVersion.setId(uploadedDraftEntity.getId());

        Assert.assertEquals(uploadedDraftEntity, draftCaptor.getValue());

        // Old draft is not dropped since its structure is changed.
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
    public void testUpdateFromFileWhenEmptyStructure() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findById(draftEntity.getId())).thenReturn(Optional.of(draftEntity));

        try {
            draftService.updateFromFile(DRAFT_ID, new UpdateFromFileRequest());
            fail(ERROR_WAITING + UserException.class.getSimpleName());

        } catch (Exception e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("version.has.not.structure", getExceptionMessage(e));
        }
    }

    /*
     * Example:
     * path = '/file/'
     * fileName = 'uploadFile'
     * extension = 'xml'
     **/
    private FileModel createFileModel(String path, String fileName, String extension) {

        String fullName = fileName + "." + extension;

        FileModel fileModel = new FileModel(fileName, fullName); // NB: fileName as path!
        fileModel.setPath(fileModel.generateFullPath());

        InputStream input = DraftServiceFileTest.class.getResourceAsStream(path + fullName);

        when(versionFileService.supply(fileModel.generateFullPath()))
                .thenReturn(() -> input)
                .thenReturn(() -> DraftServiceFileTest.class.getResourceAsStream(path + fullName))
                .thenReturn(() -> DraftServiceFileTest.class.getResourceAsStream(path + fullName))
                .thenReturn(() -> DraftServiceFileTest.class.getResourceAsStream(path + fullName));

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
    private RefBookVersionEntity createUploadedDraftEntity(RefBookEntity refBookEntity) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(null);
        entity.setRefBook(refBookEntity);
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

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // Version + Draft:
        result.put(ValidateVersionNotArchivedStrategy.class, validateVersionNotArchivedStrategy);
        result.put(ValidateDraftExistsStrategy.class, validateDraftExistsStrategy);
        result.put(FindDraftEntityStrategy.class, findDraftEntityStrategy);
        result.put(CreateDraftEntityStrategy.class, createDraftEntityStrategy);
        result.put(CreateDraftStorageStrategy.class, createDraftStorageStrategy);
        // File:

        return result;
    }
}
