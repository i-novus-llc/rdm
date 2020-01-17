package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;

import java.util.UUID;

@SuppressWarnings("squid:S1214")
public interface AsyncOperationLogEntryRepository extends JpaRepository<AsyncOperationLogEntryEntity, UUID> {

    @Transactional
    AsyncOperationLogEntryEntity findByUuid(UUID uuid);

    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "INSERT INTO n2o_rdm_management.async_log_entry (id, op_enum, payload) VALUES (:id, :op_enum, :payload) ON CONFLICT (id) DO NOTHING;")
    void saveConflictFree(@Param("id") UUID uuid, @Param("op_enum") String op, @Param("payload") String payload);

}
