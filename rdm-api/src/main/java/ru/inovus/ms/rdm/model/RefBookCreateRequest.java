package ru.inovus.ms.rdm.model;

import java.util.Map;

public class RefBookCreateRequest {

    private String code;
    private String category;
    private Map<String, String> passport;

    public RefBookCreateRequest() {
    }

    public RefBookCreateRequest(String code, Map<String, String> passport) {
        this.code = code;
        this.passport = passport;
    }

    public RefBookCreateRequest(String code, Map<String, String> passport, String category) {
        this.code = code;
        this.passport = passport;
        this.category = category;
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
