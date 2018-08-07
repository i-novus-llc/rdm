package ru.inovus.ms.rdm.file.export;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by znurgaliev on 06.08.2018.
 */
public class Archiver implements Closeable {

    private static final String TEMP_FILE_NAME_PREFIX = "zip_temp";

    private File zipFile;
    private ZipOutputStream zos;

    public Archiver() {
        try {
            this.zipFile = File.createTempFile(TEMP_FILE_NAME_PREFIX, null);
            zipFile.deleteOnExit();
            this.zos = new ZipOutputStream(new FileOutputStream(zipFile));
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not create archive", e);
        }
    }

    public Archiver addEntry(FileGenerator fileGenerator, String fileName){
        try {
            zos.putNextEntry(new ZipEntry(fileName));
            fileGenerator.generate(zos);
            zos.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Can not add generate file " + fileName, e);
        }
        return this;
    }

    public InputStream getArchive(){
        try {
            zos.flush();
            close();
            return new FileInputStream(zipFile);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() throws IOException {
        zos.close();
    }
}
