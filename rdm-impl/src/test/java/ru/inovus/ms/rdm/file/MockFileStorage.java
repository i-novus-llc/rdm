package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.FileModel;
import ru.inovus.ms.rdm.service.DraftServiceTest;

import java.io.InputStream;

public class MockFileStorage extends FileStorage {

    private FileModel fileModel;
    private String fileName;
    private String filePath;

    public void setFileModel(FileModel fileModel) {
        this.fileModel = fileModel;
        this.fileName = fileModel.getPath();
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public InputStream getContent(String path) {
        return DraftServiceTest.class.getResourceAsStream(filePath);
    }

    @Override
    public String saveContent(InputStream content, String name) {
        return (fileModel == null) ? null : fileModel.generateFullPath();
    }

    @Override
    public void removeContent(String path) {

    }
}
