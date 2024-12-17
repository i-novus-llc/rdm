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
    AsyncOperationLogEntryEntity findByUuid(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query(nativeQuery = true,
            value = """
                    INSERT INTO n2o_rdm_management.async_log_entry
                           (id, op_enum, code, payload)
                    VALUES (:id, :op_enum, :code, :payload)
                    ON CONFLICT (id) DO UPDATE SET status = 'IN_PROGRESS'
                    """)
    void saveWithoutConflict(@Param("id") UUID id, @Param("op_enum") String operationType,
                             @Param("code") String code, @Param("payload") String payload);
}
