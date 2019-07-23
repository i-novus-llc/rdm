package ru.inovus.ms.rdm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.inovus.ms.rdm.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;

import java.util.List;

public interface RefBookConflictRepository extends
        JpaRepository<RefBookConflictEntity, Integer>,
        QuerydslPredicateExecutor<RefBookConflictEntity> {

    Boolean existsByReferrerVersionId(Integer referrerVersionId);

    Boolean existsByReferrerVersionIdAndConflictType(Integer referrerVersionId, ConflictType conflictType);

    Boolean existsByReferrerVersionIdAndRefFieldCodeAndConflictType(Integer referrerVersionId, String refFieldCode, ConflictType conflictType);

    @Query("select distinct c.refRecordId from RefBookConflictEntity c\n" +
            " where c.referrerVersion.id = :referrerVersionId\n" +
            "   and c.refRecordId in (:refRecordIds)" +
            // NB: Last published versions only:
            "   and c.publishedVersion.fromDate = (\n" +
            "       select max(v.fromDate)\n" +
            "         from RefBookVersionEntity v\n" +
            "        where v.refBook.id = c.publishedVersion.refBook.id\n" +
            "          and v.status = :status\n" +
            "       )")
    List<Long> findReferrerConflictedIds(@Param("referrerVersionId") Integer referrerVersionId,
                                         @Param("refRecordIds") List<Long> refRecordIds,
                                         @Param("status") RefBookVersionStatus status);

    @Query("select distinct c.publishedVersion\n" +
            "  from RefBookConflictEntity c\n" +
            " where c.referrerVersion.id = :referrerVersionId\n" +
            "   and c.refFieldCode = :refFieldCode\n" +
            "   and c.conflictType = :conflictType\n" +
            // NB: Last versions only:
            "   and c.publishedVersion.fromDate = (\n" +
            "       select max(v.fromDate)\n" +
            "         from RefBookVersionEntity v\n" +
            "        where v.refBook.id = c.publishedVersion.refBook.id\n" +
            "       )")
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

    void deleteByReferrerVersionIdAndRefFieldCodeAndRefRecordIdIsNull(Integer referrerVersionId, String refFieldCode);
}
