package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface RefBookVersionRepository extends
        JpaRepository<RefBookVersionEntity, Integer>,
        QueryDslPredicateExecutor<RefBookVersionEntity> {

    RefBookVersionEntity findByStatusAndRefBookId(RefBookVersionStatus status, Integer refBookId);

    RefBookVersionEntity findByVersionAndRefBookCode(String version, String refBookCode);

    List<RefBookVersionEntity> findAllByStatusAndRefBookId(RefBookVersionStatus status, Integer refBookId);

    List<RefBookVersionEntity> findByStorageCode(String storageCode);

    @Query("select v from RefBookVersionEntity v where v.refBook.code = ?1 and v.fromDate <= ?2 and (v.toDate > ?2 or v.toDate is null)")
    RefBookVersionEntity findActualOnDate(String refBookCode, LocalDateTime date);

    @Query("select v from RefBookVersionEntity v where v.refBook.id = ?1 and v.status = ?2 order by v.fromDate desc")
    List<RefBookVersionEntity> findStatusVersions(Integer refBookId, RefBookVersionStatus status);

    @Query("select v from RefBookVersionEntity v where v.refBook.code = ?1 and v.status = ?2 order by v.fromDate desc")
    List<RefBookVersionEntity> findStatusVersions(String refBookCode, RefBookVersionStatus status);

    default RefBookVersionEntity findLastVersion(Integer refBookId, RefBookVersionStatus status) {
        List<RefBookVersionEntity> versions = findStatusVersions(refBookId, status);
        return ((versions != null) && !versions.isEmpty()) ? versions.get(0) : null;
    }

    default RefBookVersionEntity findLastVersion(String refBookCode, RefBookVersionStatus status) {
        List<RefBookVersionEntity> versions = findStatusVersions(refBookCode, status);
        return ((versions != null) && !versions.isEmpty()) ? versions.get(0) : null;
    }

}
