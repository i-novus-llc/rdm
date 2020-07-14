package ru.i_novus.ms.rdm.impl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.lang.NonNull;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;

public interface RefBookRepository extends
        JpaRepository<RefBookEntity, Integer>,
        QuerydslPredicateExecutor<RefBookEntity> {

    RefBookEntity findByCode(@NonNull String code);

    boolean existsById(@NonNull Integer id);
    
    boolean existsByCode(@NonNull String code);

}
