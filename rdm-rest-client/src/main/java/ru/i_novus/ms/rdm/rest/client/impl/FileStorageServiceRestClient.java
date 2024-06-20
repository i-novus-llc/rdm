package ru.i_novus.ms.rdm.rest.client.impl;

import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.service.FileStorageService;
import ru.i_novus.ms.rdm.rest.client.feign.FileStorageServiceFeignClient;

import java.io.InputStream;

public class FileStorageServiceRestClient implements FileStorageService {

    private final FileStorageServiceFeignClient client;

    public FileStorageServiceRestClient(FileStorageServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public FileModel save(InputStream is, String fileName) {
        return client.save(is, fileName);
    }
}
