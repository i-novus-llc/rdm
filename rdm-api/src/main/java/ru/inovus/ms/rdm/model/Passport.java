package ru.inovus.ms.rdm.model;

import java.util.Map;
import java.util.Objects;

/**
 * Created by znurgaliev on 02.08.2018.
 */
public class Passport {

    Map<String, String> attributes;

    public Passport() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Passport passport = (Passport) o;
        return Objects.equals(attributes, passport.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes);
    }

    public Passport(Map<String, String> pasport) {
        this.attributes = pasport;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
