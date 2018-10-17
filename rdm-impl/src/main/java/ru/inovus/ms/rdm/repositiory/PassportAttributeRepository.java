package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;

import java.util.List;

public interface PassportAttributeRepository extends
        JpaRepository<PassportAttributeEntity, Integer> {

    List<PassportAttributeEntity> findAllByComparableIsTrueOrderByPositionAsc();

}