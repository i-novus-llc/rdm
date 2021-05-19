package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class DefaultGenerateFileNameStrategyTest {

    private static final int REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";

    private static final Integer VERSION_ID = 2;
    private static final FileType FILE_TYPE = FileType.XML;

    @InjectMocks
    private DefaultGenerateFileNameStrategy strategy;

    @Test
    public void testGenerateName() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        version.setVersion("1.2");

        String fileName = strategy.generateName(version, FILE_TYPE);
        assertNotNull(fileName);
        assertTrue(fileName.contains(version.getCode()));
        assertTrue(fileName.contains(version.getVersion()));
        assertTrue(fileName.contains(FILE_TYPE.name().toLowerCase()));
    }

    @Test
    public void testGenerateNameWhenDraft() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);

        String fileName = strategy.generateName(version, FILE_TYPE);
        assertNotNull(fileName);
        assertTrue(fileName.contains(version.getCode()));
        assertTrue(fileName.contains("0.0"));
        assertTrue(fileName.contains(FILE_TYPE.name().toLowerCase()));
    }

    @Test
    public void testGenerateZipName() {

        RefBookVersion version = createVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        version.setVersion("1.2");

        String fileName = strategy.generateZipName(version, FILE_TYPE);
        assertNotNull(fileName);
        assertTrue(fileName.contains(version.getCode()));
        assertTrue(fileName.contains(version.getVersion()));
        assertTrue(fileName.contains(FILE_TYPE.name()));
        assertTrue(fileName.contains(".zip"));
    }

    private RefBookVersion createVersion() {

        RefBookVersion version = new RefBookVersion();
        version.setId(VERSION_ID);

        version.setRefBookId(REFBOOK_ID);
        version.setCode(REFBOOK_CODE);

        return version;
    }
}