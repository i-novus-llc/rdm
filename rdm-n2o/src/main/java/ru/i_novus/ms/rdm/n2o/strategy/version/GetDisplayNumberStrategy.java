package ru.i_novus.ms.rdm.n2o.strategy.version;

import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategy;

public interface GetDisplayNumberStrategy extends UiStrategy {

    String get(RefBook refBook);
}
