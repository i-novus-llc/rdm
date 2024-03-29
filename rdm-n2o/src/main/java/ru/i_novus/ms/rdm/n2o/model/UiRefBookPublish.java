package ru.i_novus.ms.rdm.n2o.model;

import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Модель публикуемого черновика справочника для UI.
 */
public class UiRefBookPublish extends UiRefBook {

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

    public UiRefBookPublish(UiRefBook uiRefBook) {
        super(uiRefBook);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        UiRefBookPublish that = (UiRefBookPublish) o;
        return Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(conflictingReferrerNames, that.conflictingReferrerNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), errorMessage, conflictingReferrerNames);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + JsonUtil.toJsonString(this);
    }
}
