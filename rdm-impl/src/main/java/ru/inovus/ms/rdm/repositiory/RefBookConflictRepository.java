package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public interface RefBookConflictRepository extends
        JpaRepository<RefBookConflictEntity, Integer>,
        QuerydslPredicateExecutor<RefBookConflictEntity> {

    Boolean existsByReferrerVersionId(Integer referrerVersionId);

    RefBookConflictEntity findByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdAndRefFieldCode(Integer referrerVersionId, Integer publishedVersionId, Long refRecordId, String refFieldCode);

    RefBookConflictEntity findFirstByReferrerVersionId(Integer referrerVersionId);
    RefBookConflictEntity findFirstByReferrerVersionIdAndConflictType(Integer referrerVersionId, ConflictType conflictType);
    RefBookConflictEntity findFirstByPublishedVersionId(Integer publishedVersionId);

    List<RefBookConflictEntity> findAllByReferrerVersionId(Integer referrerVersionId);
    List<RefBookConflictEntity> findAllByReferrerVersionIdAndConflictType(Integer referrerId, ConflictType conflictType);

    List<RefBookConflictEntity> findAllByReferrerVersionIdAndRefRecordIdIn(Integer referrerId, List<Long> refRecordIds);
}
