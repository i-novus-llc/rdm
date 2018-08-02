package ru.inovus.ms.rdm.model;

public class RefBookCreateRequest {

    private String code;
    private Passport passport;

    public RefBookCreateRequest() {
    }

    public RefBookCreateRequest(String code, Passport passport) {
        this.code = code;
        this.passport = passport;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Passport getPassport() {
        return passport;
    }

    public void setPassport(Passport passport) {
        this.passport = passport;
    }
}
