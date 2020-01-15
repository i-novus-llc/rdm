package ru.inovus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import ru.inovus.ms.rdm.api.async.Async;
import ru.inovus.ms.rdm.api.service.AsyncOperationLogEntryService;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.inovus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.util.UUID;

public class AsyncOperationLogEntryServiceImpl implements AsyncOperationLogEntryService {

    @Autowired
    private AsyncOperationLogEntryRepository repository;

    @Override
    public Async.Operation.LogEntry getById(UUID uuid) {
        return map(repository.getOne(uuid));
    }

    private Async.Operation.LogEntry map(AsyncOperationLogEntryEntity entity) {
        return null;
    }

}
