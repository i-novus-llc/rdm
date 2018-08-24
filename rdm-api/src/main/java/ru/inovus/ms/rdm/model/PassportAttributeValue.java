package ru.inovus.ms.rdm.model;

public class PassportAttributeValue {

    String value;
    String name;

    public PassportAttributeValue() {
    }

    public PassportAttributeValue(String value, String name) {

        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}