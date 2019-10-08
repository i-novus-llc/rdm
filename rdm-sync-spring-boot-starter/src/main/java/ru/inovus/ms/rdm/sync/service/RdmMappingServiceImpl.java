package ru.inovus.ms.rdm.sync.service;

import org.apache.commons.lang3.time.FastDateFormat;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;

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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
                return LocalDate.parse(value.toString(), formatter);
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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
                return LocalDate.parse(value.toString(), formatter);
            }
        } else if (clientType.equals(DataTypeEnum.VARCHAR)) {
            if (value instanceof Date) {
                return FastDateFormat.getInstance(DATE_FORMAT).format(value);
            } else if (value instanceof LocalDate || value instanceof LocalDateTime) {
                return DateTimeFormatter.ofPattern(DATE_FORMAT).format((Temporal) value);
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
        if (!(value instanceof Reference)){
            throw new ClassCastException(getClassCastError(FieldType.REFERENCE, clientType, value));
        }
        Reference reference = (Reference)value;
        String refValue = reference.getValue();
        if (refValue == null)
            return null;
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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
                return LocalDate.parse(refValue, formatter);
            case JSONB:
                return reference;
            default:
                throw new ClassCastException(getClassCastError(FieldType.REFERENCE, clientType, value));
        }
    }

    private String getClassCastError(FieldType rdmType, DataTypeEnum clientType, Object value) {
        return String.format("Ошибка при попытке преобразовать тип %s в %s значение: %s", rdmType, clientType, value);
    }

}
