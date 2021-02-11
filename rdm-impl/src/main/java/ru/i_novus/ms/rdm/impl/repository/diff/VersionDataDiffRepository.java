package ru.i_novus.ms.rdm.impl.repository.diff;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.i_novus.ms.rdm.impl.entity.diff.DataDiffSearchResult;
import ru.i_novus.ms.rdm.impl.entity.diff.VersionDataDiffEntity;

@SuppressWarnings("squid:S1214")
public interface VersionDataDiffRepository extends
        JpaRepository<VersionDataDiffEntity, Integer>,
        QuerydslPredicateExecutor<VersionDataDiffEntity> {

    String WITH_MARGIN_BY_VERSION_DIFFS = "WITH data_diff(id, primaries, rec_pos) as ( \n" +
            "  SELECT dd.id, dd.primaries, \n" +
            "    array_position(:versionDiffIds::integer[], dd.version_diff_id) as rec_pos \n" +
            "    FROM diff.version_data_diff dd \n" +
            "   WHERE dd.version_diff_id = ANY(:versionDiffIds::integer[]) \n" +
            ")\n" +
            "\n" +
            ", margin_diff(primaries, first_id, last_id) as ( \n" +
            "  SELECT DISTINCT d.primaries, \n" +
            "    first_value(d.id) over( \n" +
            "      partition by d.primaries order by d.rec_pos asc \n" +
            "      rows between unbounded preceding and unbounded following \n" +
            "    ) as first_id, \n" +
            "    last_value(d.id) over( \n" +
            "      partition by d.primaries order by d.rec_pos asc \n" +
            "      rows between unbounded preceding and unbounded following \n" +
            "    ) as last_id \n" +
            "    FROM data_diff as d \n" +
            ")\n";

    String SEARCH_BY_VERSION_DIFFS = "SELECT m.primaries as primary_values, \n" +
            "  (select d.values from diff.version_data_diff as d \n" +
            "    where d.id = m.first_id \n" +
            "  ) as first_diff_values, \n" +
            "  (case when m.last_id != m.first_id then \n" +
            "        (select d.values from diff.version_data_diff as d \n" +
            "          where d.id = m.last_id) \n" +
            "   else null end \n" +
            "  ) as last_diff_values \n" +
            "  FROM margin_diff as m \n" +
            " ORDER BY m.primaries asc";

    String COUNT_BY_VERSION_DIFFS = "SELECT count(*) FROM margin_diff as m";

    @Query(nativeQuery = true,
            value = WITH_MARGIN_BY_VERSION_DIFFS + SEARCH_BY_VERSION_DIFFS,
            countQuery = WITH_MARGIN_BY_VERSION_DIFFS + COUNT_BY_VERSION_DIFFS)
    Page<DataDiffSearchResult> searchByVersionDiffs(@Param("versionDiffIds") String versionDiffIds,
                                                    Pageable pageable);
}
