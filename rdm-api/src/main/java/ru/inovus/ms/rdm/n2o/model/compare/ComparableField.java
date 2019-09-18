package ru.inovus.ms.rdm.n2o.model.compare;

import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;

import java.util.Objects;

public class ComparableField extends RdmComparable {

    private String code;

    private String name;

    public ComparableField() {
    }

    public ComparableField(String code, String name, DiffStatusEnum status) {
        super(status);
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComparableField that = (ComparableField) o;
        if (getStatus() != null ? !getStatus().equals(that.getStatus()) : that.getStatus() != null)
            return false;
        return Objects.equals(code, that.code) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(code, name, getStatus());
    }

}
