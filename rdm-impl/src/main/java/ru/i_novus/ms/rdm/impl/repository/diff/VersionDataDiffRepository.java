package ru.i_novus.ms.rdm.impl.repository.diff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.i_novus.ms.rdm.impl.entity.diff.VersionDataDiffEntity;

@SuppressWarnings("squid:S1214")
public interface VersionDataDiffRepository extends
        JpaRepository<VersionDataDiffEntity, Integer>,
        QuerydslPredicateExecutor<VersionDataDiffEntity> {
}
