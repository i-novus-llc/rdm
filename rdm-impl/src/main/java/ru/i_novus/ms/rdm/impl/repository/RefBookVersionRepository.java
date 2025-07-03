package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings({"squid:S1214","I-novus:MethodNameWordCountRule"})
public interface RefBookVersionRepository extends
        JpaRepository<RefBookVersionEntity, Integer>,
        QuerydslPredicateExecutor<RefBookVersionEntity> {

    String SELECT_REFERRER_VERSIONS = "SELECT DISTINCT bv.*";
    String SELECT_REFERRER_VERSIONS_COUNT = "SELECT count(DISTINCT bv.*)";

    /*
     * Ссылающийся справочник должен иметь:
     *   1) структуру,
     *   2) первичный ключ,
     *   3) ссылку на указанный справочник.
     * <p/>
     * Аналогичен запросу {@link RefBookDetailModelRepository#CHECK_REFERRER_VERSION}.
     */
    String FROM_REFERRER_VERSIONS = """
              FROM n2o_rdm_management.ref_book_version AS bv
             CROSS JOIN LATERAL jsonb_to_recordset(bv."structure" -> 'attributes')
                AS akey("code" varchar, "isPrimary" bool)
             CROSS JOIN LATERAL jsonb_to_recordset(bv."structure" -> 'attributes')
                AS aref("type" varchar, "referenceCode" varchar)
             WHERE bv."structure" IS NOT NULL
               AND (bv."structure" -> 'attributes') IS NOT NULL
               AND akey."isPrimary" = TRUE
               AND aref."type" = 'REFERENCE'
               AND aref."referenceCode" = :refBookCode
            """;

    String WHERE_REF_BOOK_STATUS = """
               AND (
                     (:refBookStatus = 'ALL') OR
                     EXISTS(
                       SELECT 1
                         FROM n2o_rdm_management.ref_book AS b
                        WHERE b.id = bv.ref_book_id
                          AND ( (:refBookStatus = 'USED' AND NOT b.archived) OR
                                (:refBookStatus = 'ARCHIVED' AND b.archived)) )
                   )
            """;

    // with subquery:
    String WHERE_REF_BOOK_SOURCE = """
               AND (
                     (:refBookSource = 'ALL') OR
                     (:refBookSource = 'ACTUAL' AND bv.status = 'PUBLISHED' AND
                      bv.from_date <= timezone('utc', now()) AND
                      ( bv.to_date IS NULL OR bv.to_date > timezone('utc', now()) )) OR
                     (:refBookSource = 'DRAFT' AND bv.status = 'DRAFT') OR
                     ( (:refBookSource != 'LAST_PUBLISHED') OR
                       (:refBookSource = 'LAST_PUBLISHED' AND bv.status = 'PUBLISHED') ) AND
                       bv.id = (
                         SELECT lv.id
                           FROM n2o_rdm_management.ref_book_version AS lv
                          WHERE lv.ref_book_id = bv.ref_book_id
                            AND ( (:refBookSource = 'LAST_VERSION') OR
                                  (:refBookSource = 'LAST_PUBLISHED' AND lv.status = bv.status) )
                          ORDER BY lv.from_date DESC
                          LIMIT 1 )
                   )
            """;

    boolean existsById(@NonNull Integer id);

    RefBookVersionEntity findByStatusAndRefBookId(RefBookVersionStatus status, Integer refBookId);

    RefBookVersionEntity findByVersionAndRefBookCode(String version, String refBookCode);

    List<RefBookVersionEntity> findAllByStatusAndRefBookId(RefBookVersionStatus status, Integer refBookId);

    List<RefBookVersionEntity> findByStorageCode(String storageCode);

    @Query("select v from RefBookVersionEntity v " +
            " where v.refBook.code = ?1 and v.fromDate <= ?2 and (v.toDate > ?2 or v.toDate is null)")
    RefBookVersionEntity findActualOnDate(String refBookCode, LocalDateTime date);

    /** Последняя версия справочника с указанным статусом. */
    RefBookVersionEntity findFirstByRefBookIdAndStatusOrderByFromDateDesc(Integer refBookCode,
                                                                          RefBookVersionStatus status);

    RefBookVersionEntity findFirstByRefBookCodeAndStatusOrderByFromDateDesc(String refBookCode,
                                                                            RefBookVersionStatus status);

    List<RefBookVersionEntity> findByRefBookCodeAndStatusOrderByFromDateDesc(String refBookCode,
                                                                             RefBookVersionStatus status,
                                                                             Pageable pageable);

    List<RefBookVersionEntity> findByIdInAndStatusOrderByFromDateDesc(List<Integer> ids, RefBookVersionStatus status);

    /**
     * Поиск версий связанных справочников:
     * - ссылающихся на справочник с заданным кодом,
     * - имеющих указанный статус и тип версии справочника.
     *
     * @param refBookCode   код справочника, на который ссылаются
     * @param refBookStatus статус справочника (см. RefBookStatusType)
     * @param refBookSource тип версии справочника (см. RefBookSourceType)
     * @param pageable      информация для постраничного поиска
     * @return Страница со списком ссылающихся справочников
     */
    @Query(nativeQuery = true,
            value = SELECT_REFERRER_VERSIONS + FROM_REFERRER_VERSIONS +
                    WHERE_REF_BOOK_STATUS + WHERE_REF_BOOK_SOURCE,
            countQuery = SELECT_REFERRER_VERSIONS_COUNT + FROM_REFERRER_VERSIONS +
                    WHERE_REF_BOOK_STATUS + WHERE_REF_BOOK_SOURCE)
    Page<RefBookVersionEntity> findReferrerVersions(@Param("refBookCode") String refBookCode,
                                                    @Param("refBookStatus") String refBookStatus,
                                                    @Param("refBookSource") String refBookSource,
                                                    Pageable pageable);

    /**
     * Проверка наличия связанного справочника.
     *
     * @param refBookCode   код справочника, на который ссылаются
     * @param refBookStatus статус справочника (см. RefBookStatusType)
     * @param refBookSource тип версии справочника (см. RefBookSourceType)
     * @return Результат проверки
     */
    @Query(nativeQuery = true,
            value = "SELECT EXISTS(\n" +
                    SELECT_REFERRER_VERSIONS + FROM_REFERRER_VERSIONS +
                    WHERE_REF_BOOK_STATUS + WHERE_REF_BOOK_SOURCE +
                    ")\n")
    Boolean existsReferrerVersions(@Param("refBookCode") String refBookCode,
                                   @Param("refBookStatus") String refBookStatus,
                                   @Param("refBookSource") String refBookSource);
}
