package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.impl.entity.AsyncOperationLogEntryEntity;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("squid:S1214")
public interface AsyncOperationLogEntryRepository extends JpaRepository<AsyncOperationLogEntryEntity, UUID> {

    String GET_PUBLISHING_REF_BOOK_VERSIONS_QUERY =
            "SELECT " +
                "((payload\\:\\:json ->> 'args')\\:\\:json ->> 0)\\:\\:int " +
            "FROM " +
                "n2o_rdm_management.async_log_entry " +
            "WHERE " +
                "op_enum = 'PUBLICATION' " +
            "AND " +
                "(status = 'QUEUED' OR status = 'IN_PROGRESS') " +
            "AND " +
                "(((payload\\:\\:json ->> 'args')\\:\\:json ->> 0)\\:\\:int IN (:version_ids))";

    @Transactional
    AsyncOperationLogEntryEntity findByUuid(UUID uuid);

    @Transactional(readOnly = true)
    @Query(nativeQuery = true, value = GET_PUBLISHING_REF_BOOK_VERSIONS_QUERY)
    Set<Integer> getPublishingRefBookVersions(@Param("version_ids") List<Integer> versionIds);

}
