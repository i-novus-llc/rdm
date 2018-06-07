package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.inovus.ms.rdm.entity.RefBookEntity;

public interface RefBookRepository extends JpaRepository<RefBookEntity, Integer> {
}
