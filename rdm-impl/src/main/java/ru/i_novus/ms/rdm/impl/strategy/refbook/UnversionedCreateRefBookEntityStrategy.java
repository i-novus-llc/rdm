package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.UnversionedRefBookEntity;

@Component
public class UnversionedCreateRefBookEntityStrategy extends DefaultCreateRefBookEntityStrategy {

    @Override
    protected RefBookEntity newEntity() {
        return new UnversionedRefBookEntity();
    }
}
