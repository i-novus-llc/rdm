package ru.i_novus.ms.rdm.n2o.l10n.service;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.service.l10n.L10nService;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;

import static java.util.Collections.singletonList;
import static ru.i_novus.ms.rdm.api.util.RowUtils.prepareRowValues;

@Controller
@SuppressWarnings("unused") // used in: L10nLocalizeRecordObjectResolver
public class L10nLocalizeVersionController {

    private static final String DATA_ROW_IS_EMPTY_EXCEPTION_CODE = "data.row.is.empty";

    private final L10nService l10nService;

    @Autowired
    public L10nLocalizeVersionController(L10nService l10nService) {
        this.l10nService = l10nService;
    }

    public void localizeDataRecord(Integer versionId, Integer optLockValue, String localeCode, Row row) {

        validatePresent(row);
        prepareRowValues(row);

        LocalizeDataRequest request = new LocalizeDataRequest(optLockValue, localeCode, singletonList(row));
        l10nService.localizeData(versionId, request);
    }

    /** Проверка на заполненность хотя бы одного поля в записи. */
    private void validatePresent(Row row) {

        if (RowUtils.isEmptyRow(row))
            throw new UserException(DATA_ROW_IS_EMPTY_EXCEPTION_CODE);
    }
}
