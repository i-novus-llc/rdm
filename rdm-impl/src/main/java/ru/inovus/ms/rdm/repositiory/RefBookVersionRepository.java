package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface RefBookVersionRepository extends
        JpaRepository<RefBookVersionEntity, Integer>,
        QuerydslPredicateExecutor<RefBookVersionEntity> {

    RefBookVersionEntity findByStatusAndRefBookId(RefBookVersionStatus status, Integer refBookId);

    RefBookVersionEntity findByVersionAndRefBookCode(String version, String refBookCode);

    List<RefBookVersionEntity> findAllByStatusAndRefBookId(RefBookVersionStatus status, Integer refBookId);

    List<RefBookVersionEntity> findByStorageCode(String storageCode);

    @Query("select v from RefBookVersionEntity v where v.refBook.code = ?1 and v.fromDate <= ?2 and (v.toDate > ?2 or v.toDate is null)")
    RefBookVersionEntity findActualOnDate(String refBookCode, LocalDateTime date);

    RefBookVersionEntity findFirstByRefBookCodeAndStatusOrderByFromDateDesc(String refBookCode, RefBookVersionStatus status);

    @Query(nativeQuery = true,
            value = RefBookVersionConstants.FIND_REFERRER_VERSIONS +
                    RefBookVersionConstants.WHERE_REF_BOOK_STATUS +
                    RefBookVersionConstants.WHERE_REF_BOOK_SOURCE)
    List<RefBookVersionEntity> findReferrerVersions(@Param("refBookCode") String refBookCode,
                                                    @Param("refBookStatus") String refBookStatus,
                                                    @Param("refBookSource") String refBookSource);


    @Query(nativeQuery = true,
            value = RefBookVersionConstants.FIND_REFERRER_VERSIONS +
                    RefBookVersionConstants.WHERE_REF_BOOK_STATUS +
                    RefBookVersionConstants.WHERE_REF_BOOK_SOURCE)
    Page<RefBookVersionEntity> findReferrerVersions(@Param("refBookCode") String refBookCode,
                                                    @Param("refBookStatus") String refBookStatus,
                                                    @Param("refBookSource") String refBookSource,
                                                    Pageable pageable);
}
