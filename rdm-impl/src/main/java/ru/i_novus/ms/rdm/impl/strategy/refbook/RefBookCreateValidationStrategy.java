package ru.i_novus.ms.rdm.impl.strategy.refbook;

import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface RefBookCreateValidationStrategy extends Strategy {

    void validate(String refBookCode);
}
