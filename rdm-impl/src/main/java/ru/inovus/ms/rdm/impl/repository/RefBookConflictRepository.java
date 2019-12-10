package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.inovus.ms.rdm.impl.entity.RefBookConflictEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.api.enumeration.ConflictType;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;

import java.util.Collection;
import java.util.List;

public interface RefBookConflictRepository extends
        JpaRepository<RefBookConflictEntity, Integer>,
        QuerydslPredicateExecutor<RefBookConflictEntity> {

    /**
     * Проверка на конфликт заданного типа.
     */
    Boolean existsByReferrerVersionIdAndRefFieldCodeAndConflictType(Integer referrerVersionId, String refFieldCode, ConflictType conflictType);

    /**
     * Поиск записей данных с конфликтами указанного справочника
     * с последними опубликованными версиями справочников.
     */
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

    /**
     * Поиск последних опубликованных версий справочников для обновления ссылок.
     */
    @Query("select distinct c.publishedVersion\n" +
            "  from RefBookConflictEntity c\n" +
            " where c.referrerVersion.id = :referrerVersionId\n" +
            "   and c.refFieldCode = :refFieldCode\n" +
            "   and c.conflictType = :conflictType\n" +
            // NB: Last published versions only:
            "   and c.publishedVersion.fromDate = (\n" +
            "       select max(v.fromDate)\n" +
            "         from RefBookVersionEntity v\n" +
            "        where v.refBook.id = c.publishedVersion.refBook.id\n" +
            "          and v.status = :status\n" +
            "       )")
    List<RefBookVersionEntity> findRefreshingPublishedVersions(
            @Param("referrerVersionId") Integer referrerVersionId,
            @Param("refFieldCode") String refFieldCode,
            @Param("conflictType") ConflictType conflictType,
            @Param("status") RefBookVersionStatus status
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

    /**
     * Удаление конфликтов по заданной записи данных.
     */
    void deleteByReferrerVersionIdAndRefRecordId(Integer referrerVersionId, Long refRecordId);

    /**
     * Удаление конфликтов по заданным записям данных.
     */
    void deleteByReferrerVersionIdAndRefRecordIdIn(Integer referrerVersionId, Collection<Long> refRecordIds);

    /**
     * Удаление конфликтов данных.
     */
    void deleteByReferrerVersionIdAndRefRecordIdIsNotNull(Integer referrerVersionId);

    /**
     * Удаление конфликтов структуры для заданной ссылки.
     */
    void deleteByReferrerVersionIdAndRefFieldCodeAndRefRecordIdIsNull(Integer referrerVersionId, String refFieldCode);
}
