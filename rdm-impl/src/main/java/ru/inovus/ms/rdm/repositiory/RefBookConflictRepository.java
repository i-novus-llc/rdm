package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public interface RefBookConflictRepository extends
        JpaRepository<RefBookConflictEntity, Integer>,
        QuerydslPredicateExecutor<RefBookConflictEntity> {

    Boolean existsByReferrerVersionId(Integer referrerVersionId);

    Boolean existsByReferrerVersionIdAndConflictType(Integer referrerVersionId, ConflictType conflictType);

    Boolean existsByPublishedVersionId(Integer publishedVersionId);

    RefBookConflictEntity findByReferrerVersionIdAndPublishedVersionIdAndRefRecordIdAndRefFieldCode(
            Integer referrerVersionId, Integer publishedVersionId,
            Long refRecordId, String refFieldCode
    );

    List<RefBookConflictEntity> findAllByReferrerVersionId(Integer referrerId);

    List<RefBookConflictEntity> findAllByReferrerVersionIdAndRefRecordIdIn(Integer referrerId, List<Long> refRecordIds);

    List<RefBookConflictEntity> findAllByReferrerVersionIdAndRefFieldCodeAndConflictType(Integer referrerId, String refFieldCode, ConflictType conflictType);

    @Modifying
    @Query(nativeQuery = true,
            value = "insert into n2o_rdm_management.ref_book_conflict\n" +
                    "      (referrer_id, published_id, ref_recordid,\n" +
                    "       ref_field_code, conflict_type, creation_date)\n" +
                    "select :newReferrerVersionId, published_id, ref_recordid,\n" +
                    "       ref_field_code, conflict_type, creation_date\n" +
                    "  from n2o_rdm_management.ref_book_conflict c\n" +
                    " where referrer_id = :oldReferrerVersionId")
    void copyByReferrerVersion(@Param("oldReferrerVersionId") Integer oldReferrerVersionId,
                               @Param("newReferrerVersionId") Integer newReferrerVersionId);
}
