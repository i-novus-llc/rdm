package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.i_novus.ms.rdm.impl.entity.RefBookDetailModel;

@SuppressWarnings({"squid:S1214","java:S1192","I-novus:MethodNameWordCountRule"})
public interface RefBookDetailModelRepository extends
        JpaRepository<RefBookDetailModel, Integer>,
        QuerydslPredicateExecutor<RefBookDetailModel> {

    /**
     * Проверка наличия справочника, ссылающегося на справочник с кодом refBookCode.
     * <p/>
     * Аналогичен запросу {@link RefBookVersionRepository#FROM_REFERRER_VERSIONS}.
     */
    String CHECK_REFERRER_VERSION = """
            SELECT 1
              FROM n2o_rdm_management.ref_book_version AS bv
             CROSS JOIN LATERAL jsonb_to_recordset(bv."structure" -> 'attributes')
                AS akey("code" varchar, "isPrimary" bool)
             CROSS JOIN LATERAL jsonb_to_recordset(bv."structure" -> 'attributes')
                AS aref("type" varchar, "referenceCode" varchar)
             WHERE bv."structure" is not null
               AND (bv."structure" -> 'attributes') IS NOT NULL
               AND akey."isPrimary" = TRUE
               AND aref."type" = 'REFERENCE'
               AND aref."referenceCode" = cv.ref_book_code
             LIMIT 1
        """;

    String AND_REFERRED_IS_LAST_PUBLISHED = """
               AND c.published_id = (
                   SELECT lv.id
                     FROM n2o_rdm_management.ref_book_version AS lv
                    WHERE lv.ref_book_id = (
                          SELECT pv.ref_book_id
                            FROM n2o_rdm_management.ref_book_version AS pv
                           WHERE pv.id = c.published_id
                          )
                      AND lv.status = 'PUBLISHED'
                    ORDER BY lv.from_date DESC
                    LIMIT 1
               )
        """;

    String WITH_CURRENT_VERSION_BY_ID = """
            WITH current_version AS (
                 SELECT
                   v.id AS id,
                   b.id AS ref_book_id,
                   b.code as ref_book_code,
                   b.removable AS is_removable,
                   b.archived AS is_archived,

                   (SELECT lv.id
                      FROM n2o_rdm_management.ref_book_version AS lv
                     WHERE lv.ref_book_id = b.id AND lv.status = 'DRAFT'
                     LIMIT 1 ) AS draft_id,

                   (SELECT lv.id
                      FROM n2o_rdm_management.ref_book_version AS lv
                     WHERE lv.ref_book_id = b.id AND lv.status = 'PUBLISHED'
                     ORDER BY lv.from_date DESC
                     LIMIT 1 ) AS last_published_id

                   FROM n2o_rdm_management.ref_book_version AS v
                  INNER JOIN n2o_rdm_management.ref_book AS b
                     ON b.id = v.ref_book_id
                  WHERE v.id = :currentVersionId
            )
        """;

    String SELECT_FROM_CURRENT_VERSION = """
            SELECT
              cv.id AS current_version_id,
              cv.draft_id AS draft_version_id,
              cv.last_published_id AS last_published_version_id,

              (cv.is_removable AND NOT cv.is_archived AND
               cv.last_published_id IS NULL) AS removable,

              EXISTS(
        """ + CHECK_REFERRER_VERSION + """
              ) AS has_referrer_version,

              EXISTS(
                  SELECT 1 FROM n2o_rdm_management.ref_book_conflict AS c
                   WHERE c.referrer_id = cv.id AND c.ref_recordid IS NOT NULL
        """ + AND_REFERRED_IS_LAST_PUBLISHED + """
              ) AS has_data_conflict,

              EXISTS(
                  SELECT 1 FROM n2o_rdm_management.ref_book_conflict AS c
                   WHERE c.referrer_id = cv.id AND c.conflict_type = 'UPDATED'
        """ + AND_REFERRED_IS_LAST_PUBLISHED + """
              ) AS has_updated_conflict,

              EXISTS(
                  SELECT 1 FROM n2o_rdm_management.ref_book_conflict AS c
                   WHERE c.referrer_id = cv.id AND c.conflict_type = 'ALTERED'
        """ + AND_REFERRED_IS_LAST_PUBLISHED + """
              ) AS has_altered_conflict,

              EXISTS(
                  SELECT 1 FROM n2o_rdm_management.ref_book_conflict AS c
                   WHERE c.referrer_id = cv.id AND c.ref_recordid IS NULL
        """ + AND_REFERRED_IS_LAST_PUBLISHED + """
              ) AS has_structure_conflict,

              EXISTS(
                  SELECT 1 from n2o_rdm_management.ref_book_conflict AS c
                   WHERE c.referrer_id = cv.last_published_id
        """ + AND_REFERRED_IS_LAST_PUBLISHED + """
              ) AS last_has_conflict

              FROM current_version AS cv
        """;

    /**
     * Получение подробностей о версиях, структуре и наличии конфликтов.
     */
    @Query(nativeQuery = true,
            value = WITH_CURRENT_VERSION_BY_ID + SELECT_FROM_CURRENT_VERSION)
    RefBookDetailModel findByVersionId(@Param("currentVersionId") Integer currentVersionId);
}
