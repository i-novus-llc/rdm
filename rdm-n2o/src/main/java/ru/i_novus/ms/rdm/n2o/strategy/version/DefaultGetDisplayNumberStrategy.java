package ru.i_novus.ms.rdm.n2o.strategy.version;

import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

import static java.time.format.DateTimeFormatter.ofPattern;

@Component
public class DefaultGetDisplayNumberStrategy implements GetDisplayNumberStrategy {

    private static final String REFBOOK_DISPLAY_NUMBER_DRAFT = "refbook.display.number.draft";

    @Autowired
    private Messages messages;

    @Override
    public String get(RefBook refBook) {
        return RefBookVersionStatus.PUBLISHED.equals(refBook.getStatus())
                ? refBook.getVersion()
                : getDraftVersion(refBook);
    }

    private String getDraftVersion(RefBook refBook) {

        String draftDate = refBook.getEditDate().format(ofPattern(TimeUtils.DATE_PATTERN_EUROPEAN));
        return messages.getMessage(REFBOOK_DISPLAY_NUMBER_DRAFT, draftDate);
    }
}
