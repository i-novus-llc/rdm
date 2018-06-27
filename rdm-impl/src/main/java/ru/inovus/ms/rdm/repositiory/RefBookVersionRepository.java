package ru.inovus.ms.rdm.repositiory;


import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.RefBookVersionStatus;

import java.util.List;

@Repository
public interface RefBookVersionRepository extends
        JpaRepository<RefBookVersionEntity, Integer>,
        QueryDslPredicateExecutor<RefBookVersionEntity> {



}
