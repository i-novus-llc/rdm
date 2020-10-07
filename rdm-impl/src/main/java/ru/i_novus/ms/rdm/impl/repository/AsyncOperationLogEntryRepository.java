package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;

import java.util.UUID;

public interface AsyncOperationLogEntryRepository extends
        JpaRepository<AsyncOperationLogEntryEntity, UUID>,
        QuerydslPredicateExecutor<AsyncOperationLogEntryEntity> {

    @Transactional
    AsyncOperationLogEntryEntity findByUuid(UUID uuid);

    @Transactional
    @Modifying
    @Query(nativeQuery = true,
            value = "INSERT INTO n2o_rdm_management.async_log_entry \n" +
                    "       (id, op_enum, code, payload) \n" +
                    "VALUES (:id\\:\\:uuid, :op_enum, :code, :payload) \n" +
                    "ON CONFLICT (id) DO \n" +
                    "       UPDATE SET status = 'IN_PROGRESS'")
    void saveWithoutConflict(@Param("id") String operationId, @Param("op_enum") String operationType,
                             @Param("code") String code, @Param("payload") String payload);
}
