package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.i_novus.ms.rdm.impl.entity.RefBookModelData;

@SuppressWarnings("squid:S1214")
public interface RefBookModelDataRepository extends
        JpaRepository<RefBookModelData, Integer>,
        QuerydslPredicateExecutor<RefBookModelData> {

    String INNER_JOIN_REFERRED =
            "      inner join n2o_rdm_management.ref_book_version pv \n" +
            "         on pv.id = c.published_id \n";

    String AND_REFERRED_IS_LAST_PUBLISHED_ONLY =
            "        and c.published_id = ( \n" +
            "            select lv.id \n" +
            "              from n2o_rdm_management.ref_book_version lv \n" +
            "             where lv.ref_book_id = pv.ref_book_id \n" +
            "               and lv.status = 'PUBLISHED' \n" +
            "             order by lv.from_date desc \n" +
            "             limit 1 )\n";

    @SuppressWarnings("squid:S1192")
    String FIND_CONFLICT_DATA = "select \n" +
            "  :currentVersionId as current_version_id, \n" +
            "  null as draft_version_id, \n" +
            "  null as last_published_version_id, \n" +
            "\n" +

            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            INNER_JOIN_REFERRED +
            "     where c.referrer_id = :currentVersionId \n" +
            "       and c.ref_recordid is not null \n" +
            AND_REFERRED_IS_LAST_PUBLISHED_ONLY +
            "  ) as has_data_conflict, \n" +

            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            INNER_JOIN_REFERRED +
            "     where c.referrer_id = :currentVersionId \n" +
            "       and c.conflict_type = 'UPDATED' \n" +
            AND_REFERRED_IS_LAST_PUBLISHED_ONLY +
            "  ) as has_updated_conflict, \n" +

            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            INNER_JOIN_REFERRED +
            "     where c.referrer_id = :currentVersionId \n" +
            "       and c.conflict_type = 'ALTERED' \n" +
            AND_REFERRED_IS_LAST_PUBLISHED_ONLY +
            "  ) as has_altered_conflict, \n" +

            "  exists( \n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            INNER_JOIN_REFERRED +
            "     where c.referrer_id = :currentVersionId \n" +
            "       and c.ref_recordid is null \n" +
            AND_REFERRED_IS_LAST_PUBLISHED_ONLY +
            "  ) as has_structure_conflict, \n" +

            "\n" +
            "  (:hasLastPublishedReferrerVersion and \n" +
            "   exists( \n" +
            "     select 1 from n2o_rdm_management.ref_book_conflict c \n" +
            INNER_JOIN_REFERRED +
            "      where c.referrer_id = :lastPublishedReferrerVersionId \n" +
            AND_REFERRED_IS_LAST_PUBLISHED_ONLY +
            "   )\n" +
            "  ) as last_has_conflict \n";

    /**
     * Проверка существования конфликтов.
     *
     */
    @Query(nativeQuery = true, value = FIND_CONFLICT_DATA)
    RefBookModelData findData(@Param("currentVersionId") Integer currentVersionId,
                              @Param("hasLastPublishedReferrerVersion") boolean hasLastPublishedReferrerVersion,
                              @Param("lastPublishedReferrerVersionId") Integer lastPublishedReferrerVersionId);
}
