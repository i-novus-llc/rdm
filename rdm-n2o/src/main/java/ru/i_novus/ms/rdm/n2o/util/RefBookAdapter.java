package ru.i_novus.ms.rdm.n2o.util;

import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.RefBookOperation;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.n2o.model.UiRefBook;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.version.GetDisplayNumberStrategy;

/**
 * Адаптер модели справочника к UI.
 */
@Component
public class RefBookAdapter {

    private static final String REFBOOK_DISPLAY_STATUS_ARCHIVED = "refbook.display.status.archived";
    private static final String REFBOOK_OPERATION_PUBLISHING = "refbook.operation.publishing";
    private static final String REFBOOK_OPERATION_UPDATING = "refbook.operation.updating";

    private final UiStrategyLocator strategyLocator;

    private final Messages messages;

    @Autowired
    public RefBookAdapter(UiStrategyLocator strategyLocator,
                          Messages messages) {

        this.strategyLocator = strategyLocator;

        this.messages = messages;
    }

    public UiRefBook toUiRefBook(RefBook refBook) {

        UiRefBook result = new UiRefBook(refBook);

        result.setDisplayNumber(toDisplayNumber(refBook));
        result.setDisplayStatus(toDisplayStatus(refBook));
        result.setDisplayOperation(toDisplayOperation(refBook.getCurrentOperation()));

        return result;
    }

    private String toDisplayNumber(RefBook refBook) {

        return strategyLocator.getStrategy(refBook.getType(), GetDisplayNumberStrategy.class).get(refBook);
    }

    private String toDisplayStatus(RefBook refBook) {

        return Boolean.TRUE.equals(refBook.getArchived()) ? messages.getMessage(REFBOOK_DISPLAY_STATUS_ARCHIVED) : null;
    }

    private String toDisplayOperation(RefBookOperation operation) {

        if (operation == null)
            return null;

        return switch (operation) {
            case PUBLISHING -> messages.getMessage(REFBOOK_OPERATION_PUBLISHING);
            case UPDATING -> messages.getMessage(REFBOOK_OPERATION_UPDATING);
        };
    }
}
