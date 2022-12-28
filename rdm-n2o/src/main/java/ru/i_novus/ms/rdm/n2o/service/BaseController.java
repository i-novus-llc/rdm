package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;

/**
 * Базовый контроллер.
 */
public class BaseController {

    protected final Messages messages;

    protected BaseController(Messages messages) {

        this.messages = messages;
    }

    /**
     * Получение локализованного наименования типа перечисления.
     *
     * @param prefix префикс кода для локализации
     * @param type   тип перечисления
     * @return Локализованное наименование
     */
    protected String toEnumLocaleName(String prefix, Enum type) {
        return type != null ? messages.getMessage(prefix + type.name().toLowerCase()) : null;
    }
}
