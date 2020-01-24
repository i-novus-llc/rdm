package ru.inovus.ms.rdm.impl.service;

import com.querydsl.core.BooleanBuilder;
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
        criteria.setPageNumber(Math.max(0, criteria.getPageNumber() - 1));
        QAsyncOperationLogEntryEntity q = QAsyncOperationLogEntryEntity.asyncOperationLogEntryEntity;
        if (criteria.getSort() == null)
            criteria.setOrders(List.of(AsyncOperationLogEntryEntity.DEFAULT_ORDER));
        BooleanBuilder builder = new BooleanBuilder();
        if (criteria.getUuid() != null)
            builder.and(q.uuid.eq(criteria.getUuid()));
        if (criteria.getOperation() != null)
            builder.and(q.operation.eq(criteria.getOperation()));
        if (criteria.getStatus() != null)
            builder.and(q.status.eq(criteria.getStatus()));
        Page<AsyncOperationLogEntryEntity> page;
        if (builder.getValue() == null)
            page = repository.findAll(criteria);
        else
            page = repository.findAll(builder.getValue(), criteria);
        return page.map(this::map);
    }

    @Override
    public AsyncOperationLogEntry get(UUID uuid) {
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
