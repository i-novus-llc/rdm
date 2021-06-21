package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedGenerateFileNameStrategyTest {

    private static final int REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";

    private static final Integer VERSION_ID = 2;
    private static final FileType FILE_TYPE = FileType.XML;

    @InjectMocks
    private UnversionedGenerateFileNameStrategy strategy;

    @Test
    public void testGenerateName() {

        RefBookVersion version = createVersion();
        version.setType(RefBookTypeEnum.UNVERSIONED);
        version.setStatus(RefBookVersionStatus.PUBLISHED);
        version.setVersion("-1.0");

        String fileName = strategy.generateName(version, FILE_TYPE);
        assertNotNull(fileName);
        assertTrue(fileName.contains(version.getCode()));
        assertFalse(fileName.contains(version.getVersion()));
        assertTrue(fileName.contains(FILE_TYPE.name().toLowerCase()));
    }

    private RefBookVersion createVersion() {

        RefBookVersion version = new RefBookVersion();
        version.setId(VERSION_ID);

        version.setRefBookId(REFBOOK_ID);
        version.setCode(REFBOOK_CODE);

        return version;
    }
}