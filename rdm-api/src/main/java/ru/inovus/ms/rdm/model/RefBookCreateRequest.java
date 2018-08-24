package ru.inovus.ms.rdm.model;

import java.util.Map;

public class RefBookCreateRequest {

    private String code;
    private Map<String, PassportAttributeValue> passport;

    public RefBookCreateRequest() {
    }

    public RefBookCreateRequest(String code, Map<String, PassportAttributeValue> passport) {
        this.code = code;
        this.passport = passport;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, PassportAttributeValue> getPassport() {
        return passport;
    }

    public void setPassport(Map<String, PassportAttributeValue> passport) {
        this.passport = passport;
    }
}
