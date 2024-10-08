package ru.i_novus.ms.rdm.impl.service;

import com.querydsl.core.types.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.ExistsData;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toFieldSearchCriterias;

@RunWith(MockitoJUnitRunner.class)
public class VersionServiceTest {

    private static final String ERROR_WAITING = "Ожидается ошибка: ";

    private static final int REFBOOK_ID = 1;
    private static final String REFBOOK_CODE = "test";

    private static final int VERSION_ID = 2;
    private static final String STORAGE_CODE = "storage_code";
    private static final String ROW_HASH = "hash";
    private static final String VERSION_NUMBER = "1.0";

    private static final FileType FILE_TYPE = FileType.XML;
    private static final String FILE_PATH = "/refBook_1.0.XML.zip";

    @InjectMocks
    private VersionServiceImpl versionService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private SearchDataService searchDataService;

    @Mock
    private VersionFileService versionFileService;

    @Mock
    private AuditLogService auditLogService;

    @Test
    public void testSearch() {

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        when(searchDataService.getPagedData(any()))
                .thenReturn(new DataPage<>(0, emptyList(), new DataCriteria()));

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setAttributeFilters(new HashSet<>());
        searchDataCriteria.setCommonFilter("commonFilter");

        Page<RefBookRowValue> rowValues = versionService.search(VERSION_ID, searchDataCriteria);
        assertNotNull(rowValues);

        verify(versionRepository).findById(entity.getId());

        StorageDataCriteria dataCriteria = new StorageDataCriteria(entity.getStorageCode(),
                entity.getFromDate(), entity.getToDate(), new ArrayList<>(),
                toFieldSearchCriterias(searchDataCriteria.getAttributeFilters()), searchDataCriteria.getCommonFilter());
        dataCriteria.setPage(searchDataCriteria.getPageNumber() + DataCriteria.PAGE_SHIFT);
        dataCriteria.setSize(searchDataCriteria.getPageSize());

        verify(searchDataService).getPagedData(eq(dataCriteria));

        verifyNoMoreInteractions(versionRepository, searchDataService);
    }

    @Test
    public void testGetVersions() {

        RefBookVersionEntity entity = createVersionEntity();
        PageImpl<RefBookVersionEntity> entityPage = new PageImpl<>(List.of(entity));

        VersionCriteria criteria = new VersionCriteria();
        criteria.setId(0); // для формирования предиката поиска
        criteria.setPageNumber(0);
        criteria.setPageSize(2);

        when(versionRepository.findAll(any(Predicate.class), any(PageRequest.class))).thenReturn(entityPage);

        Page<RefBookVersion> versionPage = versionService.getVersions(criteria);
        assertNotNull(versionPage);
        assertEquals(entityPage.getTotalElements(), versionPage.getTotalElements());
        assertEquals(entityPage.getContent().size(), versionPage.getContent().size());

        RefBookVersion version = versionPage.getContent().get(0);
        assertVersion(entity, version);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(versionRepository).findAll(any(Predicate.class), captor.capture());

        PageRequest request = captor.getValue();
        assertEquals(criteria.getPageNumber(), request.getPageNumber());
        assertEquals(criteria.getPageSize(), request.getPageSize());

        verifyNoMoreInteractions(versionRepository);
    }

    @Test
    public void testGetById() {

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        RefBookVersion version = versionService.getById(entity.getId());
        assertVersion(entity, version);
    }

    @Test
    public void testGetByIdWhenNull() {

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.empty());

        final String expectedMessage = "version.not.found";
        try {
            versionService.getById(VERSION_ID);
            fail(ERROR_WAITING + expectedMessage);

        } catch (RuntimeException e) {

            assertEquals(NotFoundException.class, e.getClass());
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testGetVersion() {

        RefBookVersionEntity entity = createVersionEntity();
        entity.setVersion(VERSION_NUMBER);

        String refBookCode = entity.getRefBook().getCode();
        when(versionRepository.findByVersionAndRefBookCode(VERSION_NUMBER, refBookCode))
                .thenReturn(entity);

        RefBookVersion version = versionService.getVersion(VERSION_NUMBER, refBookCode);
        assertNotNull(version);
        assertEquals(VERSION_NUMBER, version.getVersion());
        assertEquals(refBookCode, version.getCode());
    }

    @Test
    public void testGetVersionWhenNull() {

        when(versionRepository.findByVersionAndRefBookCode(VERSION_NUMBER, REFBOOK_CODE))
                .thenReturn(null);

        final String expectedMessage = "version.with.code.and.number.not.found";
        try {
            versionService.getVersion(VERSION_NUMBER, REFBOOK_CODE);
            fail(ERROR_WAITING + expectedMessage);

        } catch (RuntimeException e) {

            assertEquals(NotFoundException.class, e.getClass());
            assertEquals(expectedMessage, e.getMessage());
        }
    }

    @Test
    public void testGetLastPublishedVersion() {

        RefBookVersionEntity entity = createVersionEntity();

        String refBookCode = entity.getRefBook().getCode();
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED))
                .thenReturn(entity);

        RefBookVersion version = versionService.getLastPublishedVersion(refBookCode);
        assertNotNull(version);
        assertEquals(refBookCode, version.getCode());
    }

    @Test
    public void testGetStructure() {

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        Structure structure = versionService.getStructure(entity.getId());
        assertEquals(entity.getStructure(), structure);
    }

    @Test
    public void testGetStorageCode() {

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        String storageCode = versionService.getStorageCode(entity.getId());
        assertEquals(entity.getStorageCode(), storageCode);
    }

    @Test
    public void testExistsData() {

        final String rowId = ROW_HASH + "$" + VERSION_ID;
        final String badFormedHash = "bad-" + ROW_HASH;
        final String nonExistentHash = "non-" + ROW_HASH;
        final String badFormedRowId = badFormedHash + "$" + "VER";
        final String nonExistentRowId = nonExistentHash + "$" + VERSION_ID;

        when(versionRepository.exists(any(Predicate.class))).thenReturn(true);

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.getOne(entity.getId())).thenReturn(entity);

        when(searchDataService.findExistentHashes(eq(entity.getStorageCode()),
                eq(entity.getFromDate()), eq(entity.getToDate()),
                eq(List.of(ROW_HASH, nonExistentHash))))
                .thenReturn(List.of(ROW_HASH));

        ExistsData expected = new ExistsData(false, List.of(badFormedRowId, nonExistentRowId));

        ExistsData actual = versionService.existsData(List.of(rowId, badFormedRowId, nonExistentRowId));
        assertEquals(expected.isExists(), actual.isExists());
        assertEquals(expected.getNotExistingRowIds(), actual.getNotExistingRowIds());
    }

    @Test
    public void testGetRow() {

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        RowValue dataRowValue = new LongRowValue(11L, emptyList());
        when(searchDataService.getData(any())).thenReturn(singletonList(dataRowValue));

        String rowId = ROW_HASH + "$" + VERSION_ID;
        RefBookRowValue rowValue = versionService.getRow(rowId);
        assertNotNull(rowValue);

        verify(versionRepository).findById(entity.getId());

        StorageDataCriteria dataCriteria = new StorageDataCriteria(entity.getStorageCode(),
                entity.getFromDate(), entity.getToDate(), new ArrayList<>());
        dataCriteria.setHashList(singletonList(ROW_HASH));

        verify(searchDataService).getData(eq(dataCriteria));

        verifyNoMoreInteractions(versionRepository, searchDataService);
    }

    @Test
    public void testGetVersionFile() {

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        when(versionFileService.getFile(any(), eq(FILE_TYPE), any(VersionService.class))).thenReturn(new ExportFile());

        ExportFile exportFile = versionService.getVersionFile(entity.getId(), FILE_TYPE);
        assertNotNull(exportFile);
    }

    @Test
    public void testGetVersionFileWhenNotFound() {

        RefBookVersionEntity entity = createVersionEntity();
        when(versionRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        when(versionFileService.getFile(any(), eq(FILE_TYPE), any(VersionService.class))).thenReturn(new ExportFile());

        ExportFile exportFile = versionService.getVersionFile(entity.getId(), FILE_TYPE);
        assertNotNull(exportFile);
    }

    private void assertVersion(RefBookVersionEntity entity, RefBookVersion version) {

        assertNotNull(version);
        assertEquals(entity.getId(), version.getId());
        assertEquals(entity.getRefBook().getId(), version.getRefBookId());
        assertEquals(entity.getRefBook().getCode(), version.getCode());
        assertEquals(entity.getStructure(), version.getStructure());
    }

    private RefBookVersionEntity createVersionEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(VERSION_ID);
        entity.setRefBook(createRefBookEntity());
        entity.setStructure(new Structure());
        entity.setStorageCode(STORAGE_CODE);
        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        entity.setFromDate(LocalDateTime.now());

        return entity;
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFBOOK_ID);
        entity.setCode(REFBOOK_CODE);

        return entity;
    }
}
