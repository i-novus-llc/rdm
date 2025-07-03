package ru.i_novus.ms.rdm.impl.file;

import lombok.experimental.UtilityClass;
import org.apache.poi.util.DefaultTempFileCreationStrategy;
import org.apache.poi.util.TempFile;

import java.io.File;

import static org.junit.Assert.assertNotNull;

@UtilityClass
public class TempFileUtil {

    private static final String TEMP_DIR_PROPERTY = "java.io.tmpdir";

    private static final String TEMP_SUBDIR_NAME = "/rdmfiles";

    public static String getTempDirProperty() {
        return TEMP_DIR_PROPERTY;
    }

    public static File getTempDir() {

        final String tempDir = System.getProperty(TEMP_DIR_PROPERTY);
        assertNotNull(tempDir);

        return new File(tempDir);
    }

    public static File getTempSubdirectory() {

        return new File(getTempDir(), TEMP_SUBDIR_NAME);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void updateTempSubdirectory() {

        final File subdir = getTempSubdirectory();
        subdir.mkdirs();

        TempFile.setTempFileCreationStrategy(new DefaultTempFileCreationStrategy(subdir));
    }
}
