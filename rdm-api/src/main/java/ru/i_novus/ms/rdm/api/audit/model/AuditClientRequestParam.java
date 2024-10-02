package ru.i_novus.ms.rdm.api.audit.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Objects;

@AllArgsConstructor
@Getter
@Setter
public class AuditClientRequestParam {

    private String value;
    private Object[] args;

    @Override
    public String toString() {
        return "AuditClientRequestParam{" +
                "value='" + value + "'" +
                ", args=" + Arrays.toString(args) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof AuditClientRequestParam))
            return false;

        AuditClientRequestParam that = (AuditClientRequestParam) o;

        return Objects.equals(getValue(), that.getValue()) &&
                Arrays.equals(getArgs(), that.getArgs());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getValue());
        result = 31 * result + Arrays.hashCode(getArgs());
        return result;
    }
}
