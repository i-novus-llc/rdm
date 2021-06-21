package ru.i_novus.ms.rdm.n2o.strategy.version;

import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;

@Component
public class UnversionedGetDisplayNumberStrategy implements GetDisplayNumberStrategy {

    private static final String REFBOOK_DISPLAY_NUMBER_UNVERSIONED = "refbook.display.number.unversioned";

    @Autowired
    private Messages messages;

    @Override
    public String get(RefBook refBook) {
        return messages.getMessage(REFBOOK_DISPLAY_NUMBER_UNVERSIONED);
    }
}
