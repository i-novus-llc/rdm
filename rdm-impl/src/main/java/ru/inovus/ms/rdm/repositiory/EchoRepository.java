package ru.inovus.ms.rdm.repositiory;

import org.springframework.data.repository.CrudRepository;
import ru.inovus.ms.rdm.entity.EchoEntity;

/**
 * Created by tnurdinov on 30.05.2018.
 */
public interface EchoRepository extends CrudRepository<EchoEntity, Long> {

    EchoEntity findOne(Long aLong);
}
