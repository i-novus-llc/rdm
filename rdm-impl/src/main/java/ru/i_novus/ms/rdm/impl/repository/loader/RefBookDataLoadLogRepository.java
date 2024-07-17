package ru.i_novus.ms.rdm.impl.repository.loader;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.lang.NonNull;
import ru.i_novus.ms.rdm.impl.entity.loader.RefBookDataLoadLogEntity;

public interface RefBookDataLoadLogRepository extends
        JpaRepository<RefBookDataLoadLogEntity, Integer>,
        QuerydslPredicateExecutor<RefBookDataLoadLogEntity> {

    boolean existsByCodeAndChangeSetId(@NonNull String code, @NonNull String changeSetId);
}
