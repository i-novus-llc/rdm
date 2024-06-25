package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.data.api.AfterDeleteDataStrategy;
import ru.i_novus.ms.rdm.impl.strategy.publish.EditPublishStrategy;

import java.util.List;

@Component
public class UnversionedAfterDeleteDataStrategy implements AfterDeleteDataStrategy {

    @Autowired
    @Qualifier("unversionedEditPublishStrategy")
    private EditPublishStrategy editPublishStrategy;

    @Override
    public void apply(RefBookVersionEntity entity, List<Object> systemIds) {

        editPublishStrategy.publish(entity);
    }
}
