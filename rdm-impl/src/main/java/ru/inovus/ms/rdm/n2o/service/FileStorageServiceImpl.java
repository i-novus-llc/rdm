package ru.inovus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.n2o.model.FileModel;
import ru.inovus.ms.rdm.n2o.service.api.FileStorageService;

import java.io.InputStream;

/**
 * Created by znurgaliev on 03.08.2018.
 */
@Service
@Primary
public class FileStorageServiceImpl implements FileStorageService {

    private FileStorage fileStorage;

    @Autowired
    public FileStorageServiceImpl(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Override
    public FileModel save(InputStream is, String fileName) {

        return new FileModel(fileStorage.saveContent(is, fileName), fileName);

    }
}
