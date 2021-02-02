package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionDiffEntity;

@SuppressWarnings("squid:S1214")
public interface RefBookVersionDiffRepository extends
        JpaRepository<RefBookVersionDiffEntity, Integer>,
        QuerydslPredicateExecutor<RefBookVersionDiffEntity> {
}
