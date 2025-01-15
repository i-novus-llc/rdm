package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.refbook.*;
import ru.i_novus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookDetailModel;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookVersionQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookDetailModelRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.publish.EditPublishStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.CreateFirstStorageStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.CreateFirstVersionStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.CreateRefBookEntityStrategy;
import ru.i_novus.ms.rdm.impl.strategy.refbook.RefBookCreateValidationStrategy;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;

import java.io.InputStream;
import java.util.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.springframework.util.CollectionUtils.isEmpty;

@RunWith(MockitoJUnitRunner.class)
public class RefBookServiceTest {

    private static final String ERROR_WAITING = "Ожидается ошибка: ";

    @InjectMocks
    private RefBookServiceImpl refBookService;

    @Mock
    private RefBookRepository refBookRepository;
    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookDetailModelRepository refBookDetailModelRepository;

    @Mock
    private DropDataService dropDataService;

    @Mock
    private RefBookLockService refBookLockService;

    @Mock
    private PassportValueRepository passportValueRepository;
    @Mock
    private RefBookVersionQueryProvider refBookVersionQueryProvider;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private DraftService draftService;
    @Mock
    private PublishService syncPublishService;

    @Mock
    private VersionFileServiceImpl versionFileService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private RefBookCreateValidationStrategy refBookCreateValidationStrategy;
    @Mock
    private CreateRefBookEntityStrategy createRefBookEntityStrategy;
    @Mock
    private CreateFirstStorageStrategy createFirstStorageStrategy;
    @Mock
    private CreateFirstVersionStrategy createFirstVersionStrategy;
    @Mock
    private ValidateVersionNotArchivedStrategy validateVersionNotArchivedStrategy;
    @Mock
    private EditPublishStrategy editPublishStrategy;

    @Before
    public void setUp() {

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategies());
        setField(refBookService, "strategyLocator", strategyLocator);
    }

    @Test
    public void testSearch() {

        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setExcludeDraft(true);

        RefBookEntity refBookEntity1 = createRefBookEntity(1);
        RefBookEntity refBookEntity2 = createRefBookEntity(2);
        List<RefBookEntity> refBookEntities = List.of(refBookEntity1, refBookEntity2);
        List<RefBookVersionEntity> versionEntities = refBookEntities.stream()
                .map(this::createRefBookVersionEntity)
                .collect(toList());

        // .search
        when(refBookVersionQueryProvider.search(criteria))
                .thenReturn(new PageImpl<>(versionEntities, criteria, versionEntities.size()));

        // .getSourceTypeVersions
        RefBookCriteria versionCriteria = new RefBookCriteria();
        versionCriteria.setSourceType(RefBookSourceType.LAST_PUBLISHED);
        versionCriteria.setRefBookIds(refBookEntities.stream().map(RefBookEntity::getId).collect(toList()));

        List<RefBookVersionEntity> lastPublishedEntities = new ArrayList<>(versionEntities);

        // .refBookModel
        when(refBookDetailModelRepository.findByVersionId(any(Integer.class)))
                .thenAnswer(v -> {
                    RefBookDetailModel data = new RefBookDetailModel();

                    Integer currentVersionId = (Integer) v.getArguments()[0];
                    data.setCurrentVersionId(currentVersionId);

                    RefBookVersionEntity lastPublishedVersion = lastPublishedEntities.stream()
                            .filter(entity -> Objects.equals(currentVersionId, entity.getId()))
                            .findFirst().orElse(null);
                    data.setLastPublishedVersion(lastPublishedVersion);

                    return data;
                });

        Page<RefBook> refBooks = refBookService.search(criteria);
        assertEquals(refBookEntities.size(), refBooks.getTotalElements());
        assertNotNull(refBooks.getContent());

        refBooks.getContent().forEach(refBook -> {

            RefBookVersionEntity expected = versionEntities.stream()
                    .filter(entity -> Objects.equals(refBook.getId(), entity.getId()))
                    .findFirst().orElse(null);
            assertNotNull(expected);
            assertEquals(expected.getRefBook().getId(), refBook.getRefBookId());

            RefBookVersionEntity lastPublishedVersion = lastPublishedEntities.stream()
                    .filter(entity -> Objects.equals(refBook.getLastPublishedVersionId(), entity.getId()))
                    .findFirst().orElse(null);
            assertNotNull(lastPublishedVersion);
        });
    }

    @Test
    public void testGetByVersionId() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        RefBookVersionEntity versionEntity = createRefBookVersionEntity(refBookEntity);

        when(versionRepository.findById(versionEntity.getId())).thenReturn(Optional.of(versionEntity));

        mockRefBookModel();

        RefBook refBook = refBookService.getByVersionId(versionEntity.getId());
        assertNotNull(refBook);
        assertEquals(versionEntity.getRefBook().getId(), refBook.getRefBookId());
    }

    @Test
    public void testGetCode() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        when(refBookRepository.getOne(refBookEntity.getId())).thenReturn(refBookEntity);

        String refBookCode = refBookService.getCode(refBookEntity.getId());
        assertEquals(refBookEntity.getCode(), refBookCode);
    }

    @Test
    public void testGetId() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        when(refBookRepository.findByCode(refBookEntity.getCode())).thenReturn(refBookEntity);

        Integer refBookId = refBookService.getId(refBookEntity.getCode());
        assertEquals(refBookEntity.getId(), refBookId);
    }

    @Test
    public void testCreate() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        RefBookVersionEntity versionEntity = createRefBookVersionEntity(refBookEntity);
        versionEntity.setStorageCode("storage_code_" + versionEntity.getId());

        RefBookCreateRequest request = new RefBookCreateRequest();
        request.setType(RefBookTypeEnum.DEFAULT);

        // .create
        when(createRefBookEntityStrategy.create(request)).thenReturn(refBookEntity);
        when(createFirstStorageStrategy.create()).thenReturn(versionEntity.getStorageCode());
        when(createFirstVersionStrategy.create(request, refBookEntity, versionEntity.getStorageCode()))
                .thenReturn(versionEntity);

        mockRefBookModel();

        RefBook refBook = refBookService.create(request);
        assertNotNull(refBook);
        assertEquals(versionEntity.getRefBook().getId(), refBook.getRefBookId());
    }

    @Test
    public void testCreateFromXls() {

        FileModel fileModel = new FileModel("filePath", "fileName.xlsx");

        final String expectedMessage = "refbook.is.not.created.from.xlsx";
        try {
            refBookService.create(fileModel);
            fail(ERROR_WAITING + expectedMessage);

        } catch (Exception e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals(expectedMessage, getExceptionMessage(e));
        }
    }

    @Test
    public void testCreateFromXml() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        RefBookVersionEntity versionEntity = createRefBookVersionEntity(refBookEntity);

        // .create
        when(createFirstVersionStrategy.create(any(RefBookCreateRequest.class), any(), any()))
                .thenReturn(versionEntity);

        mockRefBookModel();

        // .createByXml
        FileModel fileModel = createFileModel("/file/", "uploadFile", "xml");
        when(draftService.create(refBookEntity.getId(), fileModel)).thenReturn(new Draft());

        Draft draft = refBookService.create(fileModel);
        assertNotNull(draft);
    }

    @Test
    public void testUpdate() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        RefBookVersionEntity versionEntity = createRefBookVersionEntity(refBookEntity);

        // .findVersionOrThrow
        when(versionRepository.findById(versionEntity.getId())).thenReturn(Optional.of(versionEntity));

        final String updatedCode = "update_" + refBookEntity.getCode();
        final String updatedComment = "comment_" + versionEntity.getId();

        RefBookUpdateRequest request = new RefBookUpdateRequest();
        request.setVersionId(versionEntity.getId());
        request.setCode(updatedCode);
        request.setPassport(emptyMap());
        request.setComment(updatedComment);

        mockRefBookModel();

        RefBook refBook = refBookService.update(request);
        assertNotNull(refBook);
        assertEquals(versionEntity.getRefBook().getId(), refBook.getRefBookId());
        assertEquals(updatedCode, refBook.getCode());
        assertEquals(updatedComment, refBook.getComment());

        assertEquals(updatedCode, refBookEntity.getCode());
        assertEquals(updatedComment, versionEntity.getComment());
    }

    @Test
    public void testDelete() {
        
        RefBookEntity refBookEntity = createRefBookEntity(1);
        RefBookVersionEntity versionEntity = createRefBookVersionEntity(refBookEntity);
        versionEntity.setStorageCode("storage_code_" + versionEntity.getId());
        versionEntity.setPassportValues(emptyList());
        refBookEntity.setVersionList(singletonList(versionEntity));

        when(refBookRepository.getOne(refBookEntity.getId())).thenReturn(refBookEntity);
        when(versionValidation.hasReferrerVersions(refBookEntity.getCode())).thenReturn(false);

        refBookService.delete(refBookEntity.getId());

        Set<String> droppedStorageCodes = new HashSet<>(1);
        droppedStorageCodes.add(versionEntity.getStorageCode());

        verify(dropDataService).drop(droppedStorageCodes);
        verify(refBookRepository).deleteById(refBookEntity.getId());
    }

    private void mockRefBookModel() {

        // .refBookModel
        when(refBookDetailModelRepository.findByVersionId(any(Integer.class))).thenReturn(new RefBookDetailModel());
    }

    @Test
    public void testToArchive() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        when(refBookRepository.getOne(refBookEntity.getId())).thenReturn(refBookEntity);

        refBookService.toArchive(refBookEntity.getId());
        assertTrue(refBookEntity.getArchived());

        verify(refBookRepository).save(refBookEntity);
    }

    @Test
    public void testFromArchive() {

        RefBookEntity refBookEntity = createRefBookEntity(1);
        when(refBookRepository.getOne(refBookEntity.getId())).thenReturn(refBookEntity);

        refBookService.fromArchive(refBookEntity.getId());
        assertFalse(refBookEntity.getArchived());

        verify(refBookRepository).save(refBookEntity);
    }

    @Test
    public void testChangeData() {

        final RefBookEntity refBookEntity = createRefBookEntity(1);
        final String refBookCode = refBookEntity.getCode();
        final RdmChangeDataRequest request = new RdmChangeDataRequest(
                refBookCode, emptyList(), emptyList()
        );

        when(refBookRepository.findByCode(refBookCode)).thenReturn(refBookEntity);

        final RefBookVersionEntity draftEntity = createRefBookVersionEntity(refBookEntity);
        draftEntity.setStorageCode("storage_code_" + draftEntity.getId());

        final Draft draft = createDraft(draftEntity);
        when(draftService.findDraft(refBookCode)).thenReturn(draft);
        when(draftService.getDraft(draft.getId())).thenReturn(draft).thenReturn(draft);

        refBookService.changeData(request);

        verify(syncPublishService).publish(eq(draft.getId()), any(PublishRequest.class));
    }

    private RefBookEntity createRefBookEntity(Integer id) {

        final RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(id);
        entity.setCode("code_" + id);

        return entity;
    }

    private RefBookVersionEntity createRefBookVersionEntity(RefBookEntity refBookEntity) {

        return createRefBookVersionEntity(refBookEntity.getId() * 10, refBookEntity);
    }

    private RefBookVersionEntity createRefBookVersionEntity(Integer id, RefBookEntity refBookEntity) {

        final RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(id);
        entity.setRefBook(refBookEntity);

        return entity;
    }

    private Draft createDraft(RefBookVersionEntity draftEntity) {

        final Draft draft = new Draft();
        draft.setId(draftEntity.getId());
        draft.setStorageCode(draftEntity.getStorageCode());
        draft.setOptLockValue(draftEntity.getOptLockValue());

        return draft;
    }

    /*
     * Example:
     * path = '/file/'
     * fileName = 'uploadFile'
     * extension = 'xml'
     **/
    private FileModel createFileModel(String path, String fileName, String extension) {

        String fullName = fileName + "." + extension;

        FileModel fileModel = new FileModel(fileName, fullName); // fileName as path...
        fileModel.setPath(fileModel.generateFullPath()); // ...to generate right path

        InputStream input = this.getClass().getResourceAsStream(path + fullName);

        when(versionFileService.supply(fileModel.getPath()))
                .thenReturn(() -> input)
                .thenReturn(() -> this.getClass().getResourceAsStream(path + fullName))
                .thenReturn(() -> this.getClass().getResourceAsStream(path + fullName))
                .thenReturn(() -> this.getClass().getResourceAsStream(path + fullName));

        return fileModel;
    }

    /** Получение кода сообщения об ошибке из исключения. */
    private static String getExceptionMessage(Exception e) {

        if (e instanceof UserException ue) {

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
        // RefBook:
        result.put(RefBookCreateValidationStrategy.class, refBookCreateValidationStrategy);
        result.put(CreateRefBookEntityStrategy.class, createRefBookEntityStrategy);
        result.put(CreateFirstStorageStrategy.class, createFirstStorageStrategy);
        result.put(CreateFirstVersionStrategy.class, createFirstVersionStrategy);

        // Version + Draft:
        result.put(ValidateVersionNotArchivedStrategy.class, validateVersionNotArchivedStrategy);

        // Publish:
        result.put(EditPublishStrategy.class, editPublishStrategy);

        return result;
    }
}