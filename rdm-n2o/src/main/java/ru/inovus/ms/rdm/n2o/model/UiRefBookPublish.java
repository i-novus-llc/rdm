package ru.inovus.ms.rdm.n2o.model;

import ru.inovus.ms.rdm.api.model.refbook.RefBook;

import java.util.HashMap;
import java.util.Map;

/**
 * Информация о публикуемом черновике справочника для UI.
 */
public class UiRefBookPublish extends RefBook {

    /**
     * Текст сообщения об ошибке, возникшей
     * при получении дополнительной информации.
     */
    private String errorMessage;

    /**
     * Список справочников, ссылающихся на текущий справочник
     * и имеющих конфликты соответствующего типа с публикуемым черновиком.
     * <p>
     * Формат записи: <Тип конфликта> = <Список названий через запятую>
     */
    private Map<String, String> conflictingReferrerNames = new HashMap<>(0);

    public UiRefBookPublish(RefBook refBook) {
        super(refBook);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, String> getConflictingReferrerNames() {
        return conflictingReferrerNames;
    }

    public void setConflictingReferrerNames(Map<String, String> conflictingReferrerNames) {
        this.conflictingReferrerNames = conflictingReferrerNames;
    }
}
