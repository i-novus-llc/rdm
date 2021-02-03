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

@SuppressWarnings("squid:S1214")
public interface RefBookVersionRepository extends
        JpaRepository<RefBookVersionEntity, Integer>,
        QuerydslPredicateExecutor<RefBookVersionEntity> {

    /*
     * Ссылающийся справочник должен иметь:
     *   1) структуру,
     *   2) первичный ключ,
     *   3) ссылку на указанный справочник.
     *
     */
    String FIND_REFERRER_VERSIONS = "select distinct bv.* \n" +
            "  from n2o_rdm_management.ref_book_version bv \n" +
            " cross join lateral \n" +
            "       jsonb_to_recordset(bv.\"structure\" -> 'attributes') \n" +
            "           as akey(\"code\" varchar, \"isPrimary\" bool) \n" +
            " cross join lateral \n" +
            "       jsonb_to_recordset(bv.\"structure\" -> 'attributes') \n" +
            "           as aref(\"type\" varchar, \"referenceCode\" varchar) \n" +
            " where bv.\"structure\" is not null \n" +
            "   and (bv.\"structure\" -> 'attributes') is not null \n" +
            "   and akey.\"isPrimary\" = true \n" +
            "   and aref.\"type\" = 'REFERENCE' \n" +
            "   and aref.\"referenceCode\" = :refBookCode \n";

    String WHERE_REF_BOOK_STATUS = "   and ( \n" +
            "       (:refBookStatus = 'ALL') or \n" +
            "       exists( \n" +
            "         select 1 \n" +
            "           from n2o_rdm_management.ref_book b \n" +
            "          where b.id = bv.ref_book_id \n" +
            "            and ((:refBookStatus = 'USED' and not b.archived) or \n" +
            "                 (:refBookStatus = 'ARCHIVED' and b.archived)) )\n" +
            "       ) \n";

    String WHERE_REF_BOOK_SOURCE = "   and ( \n" +
            "       (:refBookSource = 'ALL') or \n" +
            "       (:refBookSource = 'ACTUAL' and bv.status = 'PUBLISHED' and \n" +
            "        bv.from_date <= timezone('utc', now()) and \n" +
            "        (bv.to_date > timezone('utc', now()) or bv.to_date is null)) or \n" +
            "       (:refBookSource = 'DRAFT' and bv.status = 'DRAFT') or \n" +
            // with subquery:
            "       (:refBookSource != 'LAST_PUBLISHED' or \n" +
            "        (:refBookSource = 'LAST_PUBLISHED' and bv.status = 'PUBLISHED')) and \n" +
            "       bv.id = ( \n" +
            "         select lv.id \n" +
            "           from n2o_rdm_management.ref_book_version lv \n" +
            "          where lv.ref_book_id = bv.ref_book_id \n" +
            "            and ( (:refBookSource = 'LAST_VERSION') or \n" +
            "                  (:refBookSource = 'LAST_PUBLISHED' and lv.status = bv.status) ) \n" +
            "          order by lv.from_date desc \n" +
            "          limit 1 )\n" +
            "       ) \n";

    boolean existsById(@NonNull Integer id);

    boolean existsByIdAndStatus(@NonNull Integer id, RefBookVersionStatus status);

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

    @Query(nativeQuery = true,
            value = FIND_REFERRER_VERSIONS + WHERE_REF_BOOK_STATUS + WHERE_REF_BOOK_SOURCE)
    Page<RefBookVersionEntity> findReferrerVersions(@Param("refBookCode") String refBookCode,
                                                    @Param("refBookStatus") String refBookStatus,
                                                    @Param("refBookSource") String refBookSource,
                                                    Pageable pageable);

    @Query(nativeQuery = true,
            value = "select exists( \n" +
                    FIND_REFERRER_VERSIONS + WHERE_REF_BOOK_STATUS + WHERE_REF_BOOK_SOURCE +
                    ") \n")
    Boolean existsReferrerVersions(@Param("refBookCode") String refBookCode,
                                   @Param("refBookStatus") String refBookStatus,
                                   @Param("refBookSource") String refBookSource);
}
