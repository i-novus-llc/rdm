package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;

import java.util.UUID;

public interface AsyncOperationLogEntryRepository extends JpaRepository<AsyncOperationLogEntryEntity, UUID>, QuerydslPredicateExecutor<AsyncOperationLogEntryEntity> {

    @Transactional
    AsyncOperationLogEntryEntity findByUuid(UUID uuid);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO n2o_rdm_management.async_log_entry (id, code, op_enum, payload) VALUES (:id, :code, :op_enum, :payload) ON CONFLICT (id) DO UPDATE SET status = 'IN_PROGRESS'")
    void saveConflictFree(@Param("id") UUID uuid, @Param("code") String code, @Param("op_enum") String op, @Param("payload") String payload);

}
