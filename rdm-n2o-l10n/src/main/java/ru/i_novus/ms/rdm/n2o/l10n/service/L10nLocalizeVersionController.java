package ru.i_novus.ms.rdm.n2o.l10n.service;

import net.n2oapp.platform.i18n.UserException;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.RowUtils;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;

@Controller
@SuppressWarnings("unused") // used in: L10nLocalizeRecordObjectResolver
public class L10nLocalizeVersionController {

    private static final String DATA_ROW_IS_EMPTY_EXCEPTION_CODE = "data.row.is.empty";

    public UiDraft localizeDataRecord(Integer versionId, Integer optLockValue, String localeCode, Row row) {

        validatePresent(row);

        // to-do: Примерный алгоритм:
        // 1. Проверка наличия локализованной версии.
        // 2. Если нет,
        //    - создание локализованной версии.
        //    - доопределение записи аналогично CreateDraftController.findNewSystemId. ????
        // 3. Сохранение записи.
        //    - REST-запрос L10nVersionStorageService.localizeData.

        return null;
    }

    /** Проверка на заполненность хотя бы одного поля в записи. */
    private void validatePresent(Row row) {
        if (RowUtils.isEmptyRow(row))
            throw new UserException(DATA_ROW_IS_EMPTY_EXCEPTION_CODE);
    }
}
