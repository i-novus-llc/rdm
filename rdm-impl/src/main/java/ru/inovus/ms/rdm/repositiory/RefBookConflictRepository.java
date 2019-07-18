package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public interface RefBookConflictRepository extends
        JpaRepository<RefBookConflictEntity, Integer>,
        QuerydslPredicateExecutor<RefBookConflictEntity> {

    Boolean existsByReferrerVersionId(Integer referrerVersionId);

    Boolean existsByReferrerVersionIdAndConflictType(Integer referrerVersionId, ConflictType conflictType);

    RefBookConflictEntity findByReferrerVersionIdAndRefRecordIdAndRefFieldCode(
            Integer referrerVersionId,
            Long refRecordId, String refFieldCode
    );

    RefBookConflictEntity findByReferrerVersionIdAndPublishedVersionIdAndRefFieldCodeAndRefRecordId(
            Integer referrerVersionId, Integer publishedVersionId, String refFieldCode, Long refRecordId
    );

    @Query("select distinct c.refRecordId from RefBookConflictEntity c \n" +
            " where c.referrerVersion.id = :referrerVersionId \n" +
            "   and c.refRecordId in (:refRecordIds)")
    List<Long> findReferrerConflictedIds(@Param("referrerVersionId") Integer referrerVersionId,
                                         @Param("refRecordIds") List<Long> refRecordIds);

    @Query("select distinct c.publishedVersion \n" +
            "  from RefBookConflictEntity c \n" +
            " where c.referrerVersion.id = :referrerVersionId \n" +
            "   and c.refFieldCode = :refFieldCode \n" +
            "   and c.conflictType = :conflictType")
    List<RefBookVersionEntity> findPublishedVersionsRefreshingByPrimary(
            @Param("referrerVersionId") Integer referrerVersionId,
            @Param("refFieldCode") String refFieldCode,
            @Param("conflictType") ConflictType conflictType
    );

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

    void deleteByReferrerVersionIdAndRefRecordId(Integer referrerVersionId, Long refRecordId);

    void deleteByReferrerVersionIdAndRefRecordIdIsNotNull(Integer referrerVersionId);
}
