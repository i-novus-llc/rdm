package ru.inovus.ms.rdm.impl.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.async.*;
import ru.inovus.ms.rdm.api.service.AsyncOperationLogEntryService;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.inovus.ms.rdm.impl.entity.QAsyncOperationLogEntryEntity;
import ru.inovus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.util.List;
import java.util.UUID;

@Service
@Primary
public class AsyncOperationLogEntryServiceImpl implements AsyncOperationLogEntryService {

    private static final List<AsyncOperation> ASYNC_OPERATION_LIST = List.of(AsyncOperation.values());
    private static final List<AsyncOperationStatus> ASYNC_OPERATION_STATUS_LIST = List.of(AsyncOperationStatus.values());

    @Autowired
    private AsyncOperationLogEntryRepository repository;

    /**
     * Поиск записей по критерию поиска.
     *
     * @param criteria критерий поиска
     * @return Страница записей
     */
    @Override
    public Page<AsyncOperationLogEntry> search(AsyncOperationLogEntryCriteria criteria) {

        Predicate predicate = toPredicate(criteria);

        Page<AsyncOperationLogEntryEntity> page = (predicate == null)
                ? repository.findAll(criteria)
                : repository.findAll(predicate, criteria);

        return page.map(this::toModel);
    }

    /**
     * Формирование предиката на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @return Предикат для запроса поиска
     */
    private static Predicate toPredicate(AsyncOperationLogEntryCriteria criteria) {

        QAsyncOperationLogEntryEntity q = QAsyncOperationLogEntryEntity.asyncOperationLogEntryEntity;
        BooleanBuilder builder = new BooleanBuilder();

        if (criteria.getUuid() != null)
            builder.and(q.uuid.eq(criteria.getUuid()));

        if (criteria.getOperation() != null)
            builder.and(q.operation.eq(criteria.getOperation()));

        if (criteria.getStatus() != null)
            builder.and(q.status.eq(criteria.getStatus()));

        return builder.getValue();
    }

    @Override
    public AsyncOperationLogEntry get(UUID uuid) {
        return toModel(repository.findById(uuid).orElse(null));
    }

    @Override
    public Page<AsyncOperation> getOpTypes() {
        return new PageImpl<>(ASYNC_OPERATION_LIST);
    }

    @Override
    public Page<AsyncOperationStatus> getStatuses() {
        return new PageImpl<>(ASYNC_OPERATION_STATUS_LIST);
    }

    private AsyncOperationLogEntry toModel(AsyncOperationLogEntryEntity entity) {
        if (entity == null)
            return null;

        AsyncOperationLogEntry logEntry = new AsyncOperationLogEntry();
        logEntry.setId(entity.getUuid());
        logEntry.setCode(entity.getCode());
        logEntry.setError(entity.getError());
        logEntry.setOperation(entity.getOperation());
        logEntry.setPayload(entity.getPayload());
        logEntry.setStatus(entity.getStatus());
        logEntry.setTsStart(entity.getTsStart());
        logEntry.setTsEnd(entity.getTsEnd());
        logEntry.setResult(entity.getResult());
        logEntry.setStackTrace(entity.getStackTrace());
        return logEntry;
    }
}
