package ru.i_novus.ms.rdm.impl.repository.diff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.i_novus.ms.rdm.impl.entity.diff.RefBookVersionDiffEntity;

@SuppressWarnings("squid:S1214")
public interface RefBookVersionDiffRepository extends
        JpaRepository<RefBookVersionDiffEntity, Integer>,
        QuerydslPredicateExecutor<RefBookVersionDiffEntity> {

    String SEARCH_VERSION_DIFF_IDS = "WITH recursive " +
            "version_diff(id, old_version_id, new_version_id, rec_sort, ids) as ( \n" +
            "  SELECT vd.id, vd.old_version_id, vd.new_version_id, \n" +
            "         1 as rec_sort, array[vd.id] as ids \n" +
            "    FROM n2o_rdm_management.ref_book_version_diff as vd \n" +
            "   WHERE vd.old_version_id = :oldVersionId \n" +
            "  union all \n" +
            "  SELECT vd.id, vd.old_version_id, vd.new_version_id, \n" +
            "         rec_sort + 1 as rec_sort, d.ids || vd.id as ids \n" +
            "    FROM n2o_rdm_management.ref_book_version_diff as vd \n" +
            "   INNER JOIN version_diff as d on d.new_version_id = vd.old_version_id \n" +
            "   WHERE vd.old_version_id = ANY(('{' || :versionIds || '}')\\:\\:integer[]) \n" +
            "     and vd.new_version_id = ANY(('{' || :versionIds || '}')\\:\\:integer[]) \n" +
            ")\n" +
            "SELECT array_to_string(d.ids, ',', 'null') as ids \n" +
            "  FROM version_diff as d \n" +
            " WHERE d.new_version_id = :newVersionId \n" +
            " ORDER BY d.rec_sort asc \n" +
            " LIMIT 1 \n";

    @Query(nativeQuery = true,
            value = SEARCH_VERSION_DIFF_IDS)
    String searchVersionDiffIds(@Param("oldVersionId") Integer oldVersionId,
                                @Param("newVersionId") Integer newVersionId,
                                @Param("versionIds") String versionIds);
}
