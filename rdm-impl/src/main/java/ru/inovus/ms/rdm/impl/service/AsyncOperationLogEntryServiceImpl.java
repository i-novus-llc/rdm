package ru.inovus.ms.rdm.impl.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.async.AsyncOperation;
import ru.inovus.ms.rdm.api.async.AsyncOperationLogEntry;
import ru.inovus.ms.rdm.api.async.AsyncOperationLogEntryCriteria;
import ru.inovus.ms.rdm.api.async.AsyncOperationStatus;
import ru.inovus.ms.rdm.api.service.AsyncOperationLogEntryService;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.inovus.ms.rdm.impl.entity.QAsyncOperationLogEntryEntity;
import ru.inovus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;
import ru.inovus.ms.rdm.impl.util.AsyncOperationLogEntryUtils;

import java.util.List;
import java.util.UUID;

@Service
@Primary
public class AsyncOperationLogEntryServiceImpl implements AsyncOperationLogEntryService {

    @Autowired
    private AsyncOperationLogEntryRepository repository;

    @Override
    public Page<AsyncOperationLogEntry> search(AsyncOperationLogEntryCriteria criteria) {
        QAsyncOperationLogEntryEntity q = QAsyncOperationLogEntryEntity.asyncOperationLogEntryEntity;
        BooleanBuilder builder = new BooleanBuilder(Expressions.TRUE);
        if (criteria.getUuid() != null)
            builder.and(q.uuid.eq(criteria.getUuid()));
        if (criteria.getOperation() != null)
            builder.and(q.operation.eq(criteria.getOperation()));
        if (criteria.getStatus() != null)
            builder.and(q.status.eq(criteria.getStatus()));
        assert builder.getValue() != null;
        return repository.findAll(builder.getValue(), criteria).map(this::map);
    }

    @Override
    public AsyncOperationLogEntry getById(UUID uuid) {
        return map(repository.findById(uuid).orElse(null));
    }

    @Override
    public Page<AsyncOperation> getOpTypes() {
        return new PageImpl<>(List.of(AsyncOperation.values()));
    }

    @Override
    public Page<AsyncOperationStatus> getStatuses() {
        return new PageImpl<>(List.of(AsyncOperationStatus.values()));
    }

    private AsyncOperationLogEntry map(AsyncOperationLogEntryEntity entity) {
        if (entity == null)
            return null;
        AsyncOperationLogEntry logEntry = new AsyncOperationLogEntry();
        logEntry.setUuid(entity.getUuid());
        logEntry.setError(entity.getError());
        logEntry.setOperation(entity.getOperation());
        logEntry.setPayload(AsyncOperationLogEntryUtils.getPayload(entity));
        logEntry.setStatus(entity.getStatus());
        logEntry.setTsStart(entity.getTsStart());
        logEntry.setTsEnd(entity.getTsEnd());
        logEntry.setResult(AsyncOperationLogEntryUtils.getResult(entity.getOperation().getResultClass(), entity));
        return logEntry;
    }

}
