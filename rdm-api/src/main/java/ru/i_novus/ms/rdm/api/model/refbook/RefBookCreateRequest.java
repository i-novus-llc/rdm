package ru.i_novus.ms.rdm.api.model.refbook;

import ru.i_novus.ms.rdm.api.util.json.JsonUtil;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class RefBookCreateRequest implements Serializable {

    /** Код справочника. */
    private String code;

    /** Категория справочника. */
    private String category;

    /** Паспорт справочника. */
    private Map<String, String> passport;

    public RefBookCreateRequest() {
        // Nothing to do.
    }

    public RefBookCreateRequest(String code, String category, Map<String, String> passport) {
        this.code = code;
        this.category = category;
        this.passport = passport;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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
                Objects.equals(category, that.category) &&
                Objects.equals(passport, that.passport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, category, passport);
    }

    @Override
    public String toString() {
        return JsonUtil.toJsonString(this);
    }
}
