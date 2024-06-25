package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.AfterDeleteAllDataStrategy;
import ru.i_novus.ms.rdm.impl.strategy.publish.EditPublishStrategy;

@Component
public class UnversionedAfterDeleteAllDataStrategy implements AfterDeleteAllDataStrategy {

    @Autowired
    @Qualifier("unversionedEditPublishStrategy")
    private EditPublishStrategy editPublishStrategy;

    @Override
    public void apply(RefBookVersionEntity entity) {

        editPublishStrategy.publish(entity);
    }
}
