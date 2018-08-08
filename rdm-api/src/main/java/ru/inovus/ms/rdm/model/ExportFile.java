package ru.inovus.ms.rdm.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;

import java.io.InputStream;

/**
 * Created by znurgaliev on 08.08.2018.
 */
@JsonIgnoreType
@JsonIgnoreProperties
public class ExportFile {


    InputStream inputStream;

    String fileName;

    public ExportFile() {
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
}
