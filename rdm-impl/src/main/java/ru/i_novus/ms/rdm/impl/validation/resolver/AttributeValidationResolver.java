package ru.i_novus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;

/**
 * Created by znurgaliev on 21.11.2018.
 */
public interface AttributeValidationResolver<T> {

    Message resolve(T value);
}
