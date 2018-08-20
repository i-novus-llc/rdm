package ru.inovus.ms.rdm.model;

import java.util.List;

public class RefBookCreateRequest {

    private String code;
    private List<PassportAttribute> passport;

    public RefBookCreateRequest() {
    }

    public RefBookCreateRequest(String code, List<PassportAttribute> passport) {
        this.code = code;
        this.passport = passport;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<PassportAttribute> getPassport() {
        return passport;
    }

    public void setPassport(List<PassportAttribute> passport) {
        this.passport = passport;
    }
}
