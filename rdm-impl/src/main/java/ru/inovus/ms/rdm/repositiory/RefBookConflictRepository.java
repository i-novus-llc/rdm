package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public interface RefBookConflictRepository extends
        JpaRepository<RefBookConflictEntity, Integer>,
        QuerydslPredicateExecutor<RefBookConflictEntity> {

    RefBookConflictEntity findByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdAndRefFieldCode(Integer referrerVersionId, Integer publishedVersionId, Long refRecordId, String refFieldCode);

    List<RefBookConflictEntity> findAllByReferrerVersionId(Integer referrerVersionId);

    RefBookConflictEntity findFirstByReferrerVersionId(Integer referrerVersionId);
    RefBookConflictEntity findFirstByReferrerVersionIdAndConflictType(Integer referrerVersionId, ConflictType conflictType);
    RefBookConflictEntity findFirstByPublishedVersionId(Integer publishedVersionId);

    List<RefBookConflictEntity> findAllByReferrerVersionIdAndConflictType(Integer referrerVersionId, ConflictType conflictType);

}
