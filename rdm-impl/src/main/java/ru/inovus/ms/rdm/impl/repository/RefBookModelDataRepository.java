package ru.inovus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.inovus.ms.rdm.impl.entity.RefBookModelData;

@SuppressWarnings("squid:S1214")
public interface RefBookModelDataRepository extends
        JpaRepository<RefBookModelData, Integer>,
        QuerydslPredicateExecutor<RefBookModelData> {

    @SuppressWarnings("squid:S1192")
    String FIND_CONFLICT_DATA = "select\n" +
            "  :referrerVersionId as referrer_version_id,\n" +
            "  null as draft_version_id,\n" +
            "  null as last_published_version_id,\n" +
            "\n" +
            "  exists(\n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c\n" +
            "     where c.referrer_id = :referrerVersionId\n" +
            "       and c.ref_recordid is not null\n" +
            "  ) as has_data_conflict,\n" +
            "  exists(\n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c\n" +
            "     where c.referrer_id = :referrerVersionId\n" +
            "       and c.conflict_type = 'UPDATED'\n" +
            "  ) as has_updated_conflict,\n" +
            "  exists(\n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c\n" +
            "     where c.referrer_id = :referrerVersionId\n" +
            "       and c.conflict_type = 'ALTERED'\n" +
            "  ) as has_altered_conflict,\n" +
            "  exists(\n" +
            "    select 1 from n2o_rdm_management.ref_book_conflict c\n" +
            "     where c.referrer_id = :referrerVersionId\n" +
            "       and c.ref_recordid is null\n" +
            "  ) as has_structure_conflict,\n" +
            "\n" +
            "  ( :hasLastPublishedVersion and\n" +
            "   exists(\n" +
            "     select 1 from n2o_rdm_management.ref_book_conflict c\n" +
            "      where c.referrer_id = :lastPublishedVersionId\n" +
            "        and c.ref_recordid is not null\n" +
            "   )\n" +
            "  ) as last_has_data_conflict \n";

    /**
     * Проверка существования конфликтов.
     *
     */
    @Query(nativeQuery = true, value = FIND_CONFLICT_DATA)
    RefBookModelData findData(@Param("referrerVersionId") Integer referrerVersionId,
                              @Param("hasLastPublishedVersion") boolean hasLastPublishedVersion,
                              @Param("lastPublishedVersionId") Integer lastPublishedVersionId);
}
