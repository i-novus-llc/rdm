package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.i_novus.ms.rdm.impl.entity.RefBookModelData;

@SuppressWarnings({"squid:S1214","java:S1192"})
public interface RefBookModelDataRepository extends
        JpaRepository<RefBookModelData, Integer>,
        QuerydslPredicateExecutor<RefBookModelData> {

    /**
     * Проверка наличия справочника, ссылащегося на справочник с кодом refBookCode.
     * <p/>
     * Аналогичен запросу {@link RefBookVersionRepository#FIND_REFERRER_VERSIONS}.
     */
    String CHECK_REFERRER_VERSION = "    select 1 \n" +
            "      from n2o_rdm_management.ref_book_version bv \n" +
            "     cross join lateral \n" +
            "           jsonb_to_recordset(bv.\"structure\" -> 'attributes') \n" +
            "               as akey(\"code\" varchar, \"isPrimary\" bool) \n" +
            "     cross join lateral \n" +
            "           jsonb_to_recordset(bv.\"structure\" -> 'attributes') \n" +
            "               as aref(\"type\" varchar, \"referenceCode\" varchar) \n" +
            "     where bv.\"structure\" is not null \n" +
            "       and (bv.\"structure\" -> 'attributes') is not null \n" +
            "       and akey.\"isPrimary\" = true \n" +
            "       and aref.\"type\" = 'REFERENCE' \n" +
            "       and aref.\"referenceCode\" = cv.ref_book_code \n" +
            "     limit 1 \n";

    String AND_REFERRED_IS_LAST_PUBLISHED = "        and c.published_id = ( \n" +
            "            select lv.id \n" +
            "              from n2o_rdm_management.ref_book_version lv \n" +
            //"             where lv.ref_book_id = pv.ref_book_id \n" +
            "             where lv.ref_book_id = ( \n" +
            "                   select pv.ref_book_id \n" +
            "                     from n2o_rdm_management.ref_book_version pv \n" +
            "                    where pv.id = c.published_id \n" +
            "                   ) \n" +
            "               and lv.status = 'PUBLISHED' \n" +
            "             order by lv.from_date desc \n" +
            "             limit 1 ) \n";

    @SuppressWarnings("squid:S1192")
    String FIND_MODEL_DATA = "with current_version as ( \n" +
            "  select \n" +
            "    v.id as id, \n" +
            "    b.id as ref_book_id, \n" +
            "    b.code as ref_book_code, \n" +
            "    b.removable as is_removable, \n" +
            "    b.archived as is_archived, \n" +
            "\n" +

            "    (select lv.id \n" +
            "       from n2o_rdm_management.ref_book_version lv \n" +
            "      where lv.ref_book_id = b.id \n" +
            "        and lv.status = 'DRAFT' \n" +
            "      limit 1 ) as draft_id, \n" +
            "\n" +

            "    (select lv.id \n" +
            "       from n2o_rdm_management.ref_book_version lv \n" +
            "      where lv.ref_book_id = b.id \n" +
            "        and lv.status = 'PUBLISHED' \n" +
            "      order by lv.from_date desc \n" +
            "      limit 1 ) as last_published_id \n" +
            "\n" +

            "    from n2o_rdm_management.ref_book_version v \n" +
            "   inner join n2o_rdm_management.ref_book b \n" +
            "      on b.id = v.ref_book_id \n" +
            "   where v.id = :currentVersionId \n" +
            ") \n" +

            "select \n" +
            "  cv.id as current_version_id, \n" +
            "  cv.draft_id as draft_version_id, \n" +
            "  cv.last_published_id as last_published_version_id, \n" +

            "  (cv.is_removable and not cv.is_archived and \n" +
            "   cv.last_published_id is null) as removable, \n" +

            "  exists(\n" + CHECK_REFERRER_VERSION + "  ) as has_referrer_version, \n" +
            "\n" +

            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            "     where c.referrer_id = cv.id \n" +
            "       and c.ref_recordid is not null \n" +
            AND_REFERRED_IS_LAST_PUBLISHED +
            "  ) as has_data_conflict, \n" +

            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            "     where c.referrer_id = cv.id \n" +
            "       and c.conflict_type = 'UPDATED' \n" +
            AND_REFERRED_IS_LAST_PUBLISHED +
            "  ) as has_updated_conflict, \n" +

            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            "     where c.referrer_id = cv.id \n" +
            "       and c.conflict_type = 'ALTERED' \n" +
            AND_REFERRED_IS_LAST_PUBLISHED +
            "  ) as has_altered_conflict, \n" +

            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            "     where c.referrer_id = cv.id \n" +
            "       and c.ref_recordid is null \n" +
            AND_REFERRED_IS_LAST_PUBLISHED +
            "  ) as has_structure_conflict, \n" +

            "\n" +
            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            "     where c.referrer_id = cv.last_published_id" +
            AND_REFERRED_IS_LAST_PUBLISHED +
            "  ) as last_has_conflict \n" +

            "  from current_version cv";

    /**
     * Проверка существования конфликтов.
     *
     */
    @Query(nativeQuery = true, value = FIND_MODEL_DATA)
    RefBookModelData findData(@Param("currentVersionId") Integer currentVersionId);
}
