package ru.i_novus.ms.rdm.api.model.refbook;

import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/** Запрос на создание справочника по параметрам. */
public class RefBookCreateRequest implements Serializable {

    /** Код справочника. */
    private String code;

    /** Тип справочника. */
    private RefBookType type;

    /** Категория справочника. */
    private String category;

    /** Паспорт справочника. */
    private Map<String, String> passport;

    public RefBookCreateRequest() {
        // Nothing to do.
    }

    public RefBookCreateRequest(String code, RefBookType type, String category, Map<String, String> passport) {

        this.code = code;
        this.type = type;
        this.category = category;
        this.passport = passport;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public RefBookType getType() {
        return type;
    }

    public void setType(RefBookType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, String> getPassport() {
        return passport;
    }

    public void setPassport(Map<String, String> passport) {
        this.passport = passport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookCreateRequest that = (RefBookCreateRequest) o;
        return Objects.equals(code, that.code) &&
                Objects.equals(type, that.type) &&
                Objects.equals(category, that.category) &&
                Objects.equals(passport, that.passport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, type, category, passport);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
