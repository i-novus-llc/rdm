package ru.inovus.ms.rdm.file.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.exception.RdmException;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by znurgaliev on 06.08.2018.
 */
public class Archiver implements Closeable {


    private static final Logger logger = LoggerFactory.getLogger(Archiver.class);
    private static final String TEMP_FILE_NAME_PREFIX = "zip_temp";

    private File zipFile;
    private ZipOutputStream zos;

    public Archiver() {
        try {
            this.zipFile = File.createTempFile(TEMP_FILE_NAME_PREFIX, null);
            zipFile.deleteOnExit();
            this.zos = new ZipOutputStream(new FileOutputStream(zipFile));
        } catch (IOException e) {
            logger.error("Can not create archive", e);
            throw new RdmException("Can not create archive");
        }
    }

    public Archiver addEntry(FileGenerator fileGenerator, String fileName){
        try(FileGenerator fg = fileGenerator) {
            zos.putNextEntry(new ZipEntry(fileName));
            fg.generate(zos);
            zos.closeEntry();
        } catch (IOException e) {
            logger.error("Can not add generate file " + fileName, e);
            throw new RdmException("Can not add generate file " + fileName);
        }
        return this;
    }

    public InputStream getArchive(){
        try {
            zos.flush();
            zos.close();
            return new CleanTempFileInputStream(zipFile);
        } catch (IOException e) {
            throw new RdmException("Archiver is closed", e);
        }
    }

    @Override
    public void close() throws IOException {
        zos.close();
    }

    private class CleanTempFileInputStream extends FileInputStream{

        public CleanTempFileInputStream(File file) throws FileNotFoundException {
            super(file);
        }

        @Override
        public void close() throws IOException {
            super.close();
            Files.delete(zipFile.toPath());
        }
    }
}
