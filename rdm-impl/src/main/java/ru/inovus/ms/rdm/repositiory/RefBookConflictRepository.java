package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.time.LocalDateTime;
import java.util.List;

public interface RefBookConflictRepository extends
        JpaRepository<RefBookConflictEntity, Integer>,
        QuerydslPredicateExecutor<RefBookConflictEntity> {

    Boolean existsByReferrerVersionId(Integer referrerId);

    RefBookConflictEntity findByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdAndRefFieldCode(Integer referrerId, Integer publishedId, Long refRecordId, String refFieldCode);

    List<RefBookConflictEntity> findAllByReferrerVersionId(Integer referrerId);

    List<RefBookConflictEntity> findAllByReferrerVersionIdAndConflictType(Integer referrerId, ConflictType conflictType);

    List<RefBookConflictEntity> findAllByReferrerVersionIdAndRefRecordIdIn(Integer referrerId, List<Long> refRecordIds);

    @Modifying
    @Query("update RefBookConflictEntity c set c.handlingDate = :handlingDate where c.id = :id")
    void setHandlingDate(@Param("id") Integer id, @Param("handlingDate")LocalDateTime handlingDate);

}
