package ru.inovus.ms.rdm.api.model.refbook;

import java.util.Map;

public class RefBookCreateRequest {

    /** Код справочника. */
    private String code;

    /** Категория справочника. */
    private String category;

    /** Паспорт справочника. */
    private Map<String, String> passport;

    public RefBookCreateRequest() {
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
}
