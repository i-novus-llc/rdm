package ru.i_novus.ms.rdm.sync.service;

import org.apache.commons.lang3.time.FastDateFormat;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Date;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public class RdmMappingServiceImpl implements RdmMappingService {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    @Override
    public Object map(FieldType rdmType, DataTypeEnum clientType, Object value) {

        if (value == null) {
            return FieldType.BOOLEAN.equals(rdmType) ? mapBoolean(clientType, value) : null;
        }

        Object result = null;
        switch (rdmType) {
            case STRING:
                return mapVarchar(clientType, value);
            case INTEGER:
                return mapInteger(clientType, value);
            case BOOLEAN:
                return mapBoolean(clientType, value);
            case FLOAT:
                return mapFloat(clientType, value);
            case DATE:
                return mapDate(clientType, value);
            case TREE:
                return value.toString();
            case REFERENCE:
                return mapReference(clientType, value);
        }

        return result;
    }

    private Object mapInteger(DataTypeEnum clientType, Object value) {
        switch (clientType) {
            case INTEGER:
                return new BigInteger(value.toString());
            case VARCHAR:
                return value.toString();
            case FLOAT:
                return Float.parseFloat(value.toString());
            default:
                throw new ClassCastException(getClassCastError(FieldType.INTEGER, clientType, value));
        }
    }

    private Object mapVarchar(DataTypeEnum clientType, Object value) {
        switch (clientType) {
            case VARCHAR:
                return value;
            case INTEGER:
                return new BigInteger(value.toString());
            case FLOAT:
                return Float.parseFloat(value.toString());
            case BOOLEAN:
                return Boolean.parseBoolean(value.toString());
            case DATE:
                return LocalDate.parse(value.toString(), DATE_FORMATTER);
            default:
                throw new ClassCastException(getClassCastError(FieldType.STRING, clientType, value));
        }
    }

    private Object mapDate(DataTypeEnum clientType, Object value) {
        if (clientType.equals(DataTypeEnum.DATE)) {
            if (value instanceof LocalDate)
                return value;
            else if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate();
            } else if (value instanceof Date) {
                return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                return LocalDate.parse(value.toString(), DATE_FORMATTER);
            }
        } else if (clientType.equals(DataTypeEnum.VARCHAR)) {
            if (value instanceof Date) {
                return FastDateFormat.getInstance(DATE_FORMAT).format(value);
            } else if (value instanceof LocalDate || value instanceof LocalDateTime) {
                return DATE_FORMATTER.format((Temporal) value);
            } else {
                throw new ClassCastException(getClassCastError(FieldType.DATE, clientType, value));
            }
        } else {
            throw new ClassCastException(getClassCastError(FieldType.DATE, clientType, value));
        }
    }

    private Object mapBoolean(DataTypeEnum clientType, Object value) {
        if (value == null)
            value = "false";
        if (clientType.equals(DataTypeEnum.VARCHAR)) {
            return value.toString();
        } else if (clientType.equals(DataTypeEnum.BOOLEAN)) {
            return Boolean.parseBoolean(value.toString());
        } else {
            throw new ClassCastException(getClassCastError(FieldType.BOOLEAN, clientType, value));
        }
    }

    private Object mapFloat(DataTypeEnum clientType, Object value) {
        if (clientType.equals(DataTypeEnum.VARCHAR)) {
            return value.toString();
        } else if (clientType.equals(DataTypeEnum.FLOAT)) {
            return Float.parseFloat(value.toString());
        } else {
            throw new ClassCastException(getClassCastError(FieldType.FLOAT, clientType, value));
        }
    }

    private Object mapReference(DataTypeEnum clientType, Object value) {
        String refValue;
        Reference reference = null;
        if (value instanceof Reference) {
            reference = (Reference) value;
            refValue = reference.getValue();
        } else {
            if (value == null)
                return null;
            refValue = value.toString();
        }
        switch (clientType) {
            case VARCHAR:
                return refValue;
            case INTEGER:
                return new BigInteger(refValue);
            case FLOAT:
                return Float.parseFloat(refValue);
            case BOOLEAN:
                return Boolean.parseBoolean(refValue);
            case DATE:
                return LocalDate.parse(refValue, DATE_FORMATTER);
            case JSONB:
                return reference == null ? refValue : reference;
            default:
                throw new ClassCastException(getClassCastError(FieldType.REFERENCE, clientType, value));
        }
    }

    private String getClassCastError(FieldType rdmType, DataTypeEnum clientType, Object value) {
        return String.format("Error while casting %s to %s. Value: %s", rdmType, clientType, value);
    }

}
