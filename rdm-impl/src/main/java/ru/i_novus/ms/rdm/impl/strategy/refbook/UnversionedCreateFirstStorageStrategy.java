package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.platform.datastorage.temporal.service.StorageService;

import static java.util.Collections.emptyList;

@Component
public class UnversionedCreateFirstStorageStrategy extends DefaultCreateFirstStorageStrategy {

    @Autowired
    private StorageService storageService;

    @Override
    @Transactional
    public String create() {
        return storageService.createStorage(emptyList());
    }
}
