package ru.inovus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.async.Async;
import ru.inovus.ms.rdm.api.service.AsyncOperationLogEntryService;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.inovus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;
import ru.inovus.ms.rdm.impl.util.AsyncOperationLogEntryUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AsyncOperationLogEntryServiceImpl implements AsyncOperationLogEntryService {



    @Autowired
    private AsyncOperationLogEntryRepository repository;

    @Override
    public Async.Operation.LogEntry getById(UUID uuid) {
        return map(repository.getOne(uuid));
    }

    @Override
    public Set<Integer> getPublishingRefBookVersions(List<Integer> versionIds) {
        return repository.getPublishingRefBookVersions(versionIds);
    }

    private Async.Operation.LogEntry map(AsyncOperationLogEntryEntity entity) {
        Async.Operation.LogEntry logEntry = new Async.Operation.LogEntry();
        logEntry.setUuid(entity.getUuid());
        logEntry.setError(entity.getError());
        logEntry.setOperation(entity.getOperation());
        logEntry.setPayload(AsyncOperationLogEntryUtils.getPayload(entity));
        logEntry.setStatus(entity.getStatus());
        logEntry.setTsStartUTC(entity.getTsStartUTC());
        logEntry.setTsEndUTC(entity.getTsEndUTC());
        logEntry.setResult(AsyncOperationLogEntryUtils.getResult(entity.getOperation().getResultClass(), entity));
        return logEntry;
    }

}
