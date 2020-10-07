package ru.i_novus.ms.rdm.impl.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.async.*;
import ru.i_novus.ms.rdm.api.service.AsyncOperationLogEntryService;
import ru.i_novus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;
import ru.i_novus.ms.rdm.impl.entity.QAsyncOperationLogEntryEntity;
import ru.i_novus.ms.rdm.impl.repository.AsyncOperationLogEntryRepository;

import java.util.List;
import java.util.UUID;

@Service
@Primary
public class AsyncOperationLogEntryServiceImpl implements AsyncOperationLogEntryService {

    private static final List<AsyncOperationTypeEnum> ASYNC_OPERATION_LIST = List.of(AsyncOperationTypeEnum.values());
    private static final List<AsyncOperationStatusEnum> ASYNC_OPERATION_STATUS_LIST = List.of(AsyncOperationStatusEnum.values());

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

        criteria.setPageNumber(Math.max(-1, criteria.getPageNumber() - 1));

        QAsyncOperationLogEntryEntity q = QAsyncOperationLogEntryEntity.asyncOperationLogEntryEntity;
        BooleanBuilder builder = new BooleanBuilder();

        if (criteria.getId() != null)
            builder.and(q.uuid.eq(criteria.getId()));

        if (criteria.getOperationType() != null)
            builder.and(q.operationType.eq(criteria.getOperationType()));

        if (criteria.getCode() != null)
            builder.and(q.code.startsWithIgnoreCase(criteria.getCode().toUpperCase()));

        if (criteria.getStatus() != null)
            builder.and(q.status.eq(criteria.getStatus()));

        return builder.getValue();
    }

    @Override
    public AsyncOperationLogEntry get(UUID id) {
        return toModel(repository.findByUuid(id));
    }

    @Override
    public Page<AsyncOperationTypeEnum> getOperationTypes() {
        return new PageImpl<>(ASYNC_OPERATION_LIST);
    }

    @Override
    public Page<AsyncOperationStatusEnum> getOperationStatuses() {
        return new PageImpl<>(ASYNC_OPERATION_STATUS_LIST);
    }

    private AsyncOperationLogEntry toModel(AsyncOperationLogEntryEntity entity) {

        if (entity == null)
            return null;

        AsyncOperationLogEntry model = new AsyncOperationLogEntry();
        model.setId(entity.getUuid());
        model.setOperationType(entity.getOperationType());
        model.setCode(entity.getCode());

        model.setStatus(entity.getStatus());
        model.setTsStart(entity.getTsStart());
        model.setTsEnd(entity.getTsEnd());

        model.setPayload(entity.getPayload());
        model.setResult(entity.getResult());
        model.setError(entity.getError());
        model.setStackTrace(entity.getStackTrace());

        return model;
    }
}
