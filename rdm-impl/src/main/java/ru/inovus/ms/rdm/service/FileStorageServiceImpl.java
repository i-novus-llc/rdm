package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.service.api.FileStorageService;

import java.io.InputStream;

/**
 * Created by znurgaliev on 03.08.2018.
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private FileStorage fileStorage;

    @Autowired
    public FileStorageServiceImpl(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Override
    public String save(InputStream is, String fileName) {

        return fileStorage.saveContent(is, fileName);

    }
}
