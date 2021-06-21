package ru.i_novus.ms.rdm.impl.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.i_novus.ms.rdm.impl.file.export.XmlFileGenerator;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.file.AllowStoreVersionFileStrategy;
import ru.i_novus.ms.rdm.impl.strategy.file.GenerateFileNameStrategy;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VersionFileServiceTest {

    private static final int REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";

    private static final Integer VERSION_ID = 2;
    private static final FileType FILE_TYPE = FileType.XML;
    private static final String FILE_NAME = "refBook_1.0.xml";
    private static final String ZIP_NAME = "refBook_1.0.XML.zip";
    private static final String FILE_PATH = "/" + ZIP_NAME;

    @InjectMocks
    private VersionFileServiceImpl versionFileService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private VersionFileRepository versionFileRepository;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private PerRowFileGeneratorFactory fileGeneratorFactory;

    @Mock
    private XmlFileGenerator fileGenerator;

    @Mock
    private PassportValueRepository passportValueRepository;

    @Mock
    private AllowStoreVersionFileStrategy allowStoreVersionFileStrategy;

    @Mock
    private GenerateFileNameStrategy generateFileNameStrategy;

    @Mock
    private VersionService versionService;

    @Before
    public void setUp() throws Exception {

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategiesMap());
        FieldSetter.setField(versionFileService, VersionFileServiceImpl.class.getDeclaredField("strategyLocator"), strategyLocator);
    }

    @Test
    public void testCreate() {

        RefBookVersion version = createVersion();

        // .generate
        when(fileGeneratorFactory.getFileGenerator(any(), eq(version), eq(FILE_TYPE))).thenReturn(fileGenerator);
        when(generateFileNameStrategy.generateName(eq(version), eq(FILE_TYPE))).thenReturn(FILE_NAME);

        // .create
        when(generateFileNameStrategy.generateZipName(eq(version), eq(FILE_TYPE))).thenReturn(ZIP_NAME);
        when(fileStorage.saveContent(any(InputStream.class), eq(ZIP_NAME))).thenReturn(FILE_PATH);

        String actualPath = versionFileService.create(version, FileType.XML, versionService);
        assertEquals(FILE_PATH, actualPath);

        verify(fileStorage).saveContent(any(InputStream.class), eq(ZIP_NAME));
        verifyNoMoreInteractions(fileStorage);
    }

    @Test
    public void testSave() {

        RefBookVersion version = createVersion();
        InputStream is = mock(InputStream.class);

        when(generateFileNameStrategy.generateZipName(eq(version), eq(FILE_TYPE))).thenReturn(ZIP_NAME);
        when(fileStorage.saveContent(eq(is), eq(ZIP_NAME))).thenReturn(FILE_PATH);

        versionFileService.save(version, FILE_TYPE, is);

        verify(versionRepository).getOne(version.getId());

        VersionFileEntity insertedEntity = new VersionFileEntity();
        insertedEntity.setType(FILE_TYPE);
        insertedEntity.setPath(FILE_PATH);
        verify(versionFileRepository).save(insertedEntity);
    }

    @Test
    public void testGetFileWhenExist() {

        // .getFile
        RefBookVersion version = createVersion();
        when(allowStoreVersionFileStrategy.allow(version)).thenReturn(true);

        VersionFileEntity fileEntity = new VersionFileEntity();
        fileEntity.setPath(FILE_PATH);
        when(versionFileRepository.findByVersionIdAndType(version.getId(), FILE_TYPE)).thenReturn(fileEntity);
        when(fileStorage.isExistContent(FILE_PATH)).thenReturn(true);

        // .buildExportFile
        InputStream is = mock(InputStream.class);
        when(fileStorage.getContent(FILE_PATH)).thenReturn(is);
        when(generateFileNameStrategy.generateZipName(eq(version), eq(FILE_TYPE))).thenReturn(ZIP_NAME);

        ExportFile expected = new ExportFile(is, ZIP_NAME);
        ExportFile actual = versionFileService.getFile(version, FILE_TYPE, versionService);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetFileWhenAbsentInsert() {

        // .getFile
        RefBookVersion version = createVersion();
        when(allowStoreVersionFileStrategy.allow(version)).thenReturn(true);

        when(versionFileRepository.findByVersionIdAndType(version.getId(), FILE_TYPE))
                .thenReturn(null) // findFilePath
                .thenReturn(null); // saveEntity

        // .generate
        when(fileGeneratorFactory.getFileGenerator(any(), eq(version), eq(FILE_TYPE))).thenReturn(fileGenerator);
        when(generateFileNameStrategy.generateName(eq(version), eq(FILE_TYPE))).thenReturn(FILE_NAME);

        when(generateFileNameStrategy.generateZipName(eq(version), eq(FILE_TYPE)))
                .thenReturn(ZIP_NAME) // for: create
                .thenReturn(ZIP_NAME); // for: buildExportFile

        // .create
        when(fileStorage.saveContent(any(InputStream.class), eq(ZIP_NAME))).thenReturn(FILE_PATH);

        // .createOrThrow
        when(fileStorage.isExistContent(FILE_PATH)).thenReturn(true);

        // .buildExportFile
        InputStream is = mock(InputStream.class);
        when(fileStorage.getContent(FILE_PATH)).thenReturn(is);

        ExportFile expected = new ExportFile(is, ZIP_NAME);
        ExportFile actual = versionFileService.getFile(version, FILE_TYPE, versionService);
        assertEquals(expected, actual);

        // .saveEntity
        verify(versionRepository).getOne(version.getId());

        VersionFileEntity insertedEntity = new VersionFileEntity();
        insertedEntity.setType(FILE_TYPE);
        insertedEntity.setPath(FILE_PATH);
        verify(versionFileRepository).save(insertedEntity);
    }

    @Test
    public void testGetFileWhenAbsentUpdate() {

        // .getFile
        RefBookVersion version = createVersion();
        when(allowStoreVersionFileStrategy.allow(version)).thenReturn(true);

        VersionFileEntity fileEntity = new VersionFileEntity();
        fileEntity.setPath(FILE_PATH);
        when(versionFileRepository.findByVersionIdAndType(version.getId(), FILE_TYPE))
                .thenReturn(null) // findFilePath
                .thenReturn(fileEntity); // saveEntity

        // .generate
        when(fileGeneratorFactory.getFileGenerator(any(), eq(version), eq(FILE_TYPE))).thenReturn(fileGenerator);
        when(generateFileNameStrategy.generateName(eq(version), eq(FILE_TYPE))).thenReturn(FILE_NAME);

        when(generateFileNameStrategy.generateZipName(eq(version), eq(FILE_TYPE)))
                .thenReturn(ZIP_NAME) // for: create
                .thenReturn(ZIP_NAME); // for: buildExportFile

        // .create
        when(fileStorage.saveContent(any(InputStream.class), eq(ZIP_NAME))).thenReturn(FILE_PATH);

        // .createOrThrow
        when(fileStorage.isExistContent(FILE_PATH)).thenReturn(true);

        // .buildExportFile
        InputStream is = mock(InputStream.class);
        when(fileStorage.getContent(FILE_PATH)).thenReturn(is);

        ExportFile expected = new ExportFile(is, ZIP_NAME);
        ExportFile actual = versionFileService.getFile(version, FILE_TYPE, versionService);
        assertEquals(expected, actual);

        verifyNoMoreInteractions(versionRepository);

        // .saveEntity
        VersionFileEntity updatedEntity = new VersionFileEntity();
        updatedEntity.setType(FILE_TYPE);
        updatedEntity.setPath(FILE_PATH);
        verify(versionFileRepository).save(any(VersionFileEntity.class));
    }

    private RefBookVersion createVersion() {

        RefBookVersion version = new RefBookVersion();
        version.setId(VERSION_ID);

        version.setRefBookId(REFBOOK_ID);
        version.setCode(REFBOOK_CODE);

        return version;
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategiesMap() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        result.put(AllowStoreVersionFileStrategy.class, allowStoreVersionFileStrategy);
        result.put(GenerateFileNameStrategy.class, generateFileNameStrategy);

        return result;
    }
}