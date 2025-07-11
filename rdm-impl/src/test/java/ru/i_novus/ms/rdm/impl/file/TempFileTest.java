package ru.i_novus.ms.rdm.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.TempFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TempFileTest {

    static {
        System.setProperty("logging.level.ru.i_novus.ms.rdm.impl.file.TempFileTest","INFO");
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testTempDir() {

        final String tempDirProperty = TempFileUtil.getTempDirProperty();
        final File tempDir = TempFileUtil.getTempDir();

        testDirectory(tempDirProperty, tempDir);
    }

    @Test
    public void testTempRoot() {

        final File tempRoot = tempFolder.getRoot();
        testDirectory("temp folder", tempRoot);
    }

    @Test
    public void testTempPoiFile() {

        testCreateApachePoiTempFile();
    }

    private void testDirectory(String name, File directory) {

        log.info("Directory: {}={}", name, directory);
        assertDirectory(directory);

        testCreateFile(directory);
        testCreateJavaIoTempFile(directory);
    }

    private void testCreateFile(File directory) {

        final File file = new File(directory, "testfile.ext");
        try {
            createFile(file);
            assertFile(file);

        } finally {
            deleteFile(file);
        }
    }

    private void testCreateJavaIoTempFile(File directory) {

        File file = null;
        try {
            file = File.createTempFile("tempfile", ".ext", directory);
            log.info("java.io: Temp file '{}' created successfully", file);

            assertFile(file);

        } catch (IOException e) {
            log.error("java.io: Error creating temp file '{}':\n{}", file, e.getMessage());
            throw new RuntimeException(e);

        } finally {
            deleteFile(file);
        }
    }

    private void testCreateApachePoiTempFile() {

        TempFileUtil.updateTempSubdirectory();

        File file = null;
        try {
            file = TempFile.createTempFile("poifile", ".ext");
            log.info("apache.poi: Temp file '{}' created successfully", file);

            assertFile(file);

        } catch (IOException e) {
            log.error("apache.poi: Error creating temp file '{}':\n{}", file, e.getMessage());
            throw new RuntimeException(e);

        } finally {
            deleteFile(file);
        }
    }

    private static void createFile(File file) {
        try {
            final boolean created = file.createNewFile();
            if (created)
                log.info("File '{}' created successfully", file);
            else
                log.info("File '{}' already exists", file);

        } catch (IOException e) {
            log.error("Error creating file '{}':\n{}", file, e.getMessage());
            throw new RuntimeException(e);

        }
    }

    private static void deleteFile(File file) {

        if (file == null || !file.exists())
            return;

        final boolean deleted = file.delete();
        if (deleted)
            log.info("File '{}' deleted successfully", file);
        else
            log.info("File '{}' cannot be deleted", file);
    }

    private static void assertFile(File file) {

        assertNotNull(file);

        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertTrue(file.canRead());
        assertTrue(file.canWrite());
    }

    private static void assertDirectory(File directory) {

        assertNotNull(directory);

        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
        assertTrue(directory.canRead());
        assertTrue(directory.canWrite());
    }
}
