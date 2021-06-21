package ru.i_novus.ms.rdm.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

import java.io.InputStream;
import java.util.Objects;

/**
 * Created by znurgaliev on 08.08.2018.
 */
@JsonIgnoreType
@JsonIgnoreProperties
public class ExportFile {

    private InputStream inputStream;

    private String fileName;

    public ExportFile() {
        // Nothing to do.
    }

    public ExportFile(InputStream inputStream, String fileName) {

        this.inputStream = inputStream;
        this.fileName = fileName;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExportFile that = (ExportFile) o;
        return Objects.equals(inputStream, that.inputStream) &&
                Objects.equals(fileName, that.fileName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(inputStream, fileName);
    }
}
