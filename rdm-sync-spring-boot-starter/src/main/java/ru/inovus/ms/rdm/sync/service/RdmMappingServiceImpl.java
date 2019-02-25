package ru.inovus.ms.rdm.sync.service;

import org.apache.commons.lang3.time.FastDateFormat;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;
import ru.inovus.ms.rdm.sync.model.FieldMapping;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Date;

/**
 * @author lgalimova
 * @since 21.02.2019
 */
public class RdmMappingServiceImpl implements RdmMappingService {
    private static final String DATE_FORMAT = "dd.MM.yyyy";


    @Override
    public Object map(FieldMapping fieldMapping, Object value) {
        if (value == null) {
            return null;
        }
        DataTypeEnum rdmType = DataTypeEnum.getByText(fieldMapping.getRdmDataType());
        DataTypeEnum sysType = DataTypeEnum.getByText(fieldMapping.getSysDataType());
        if (rdmType == null || sysType == null)
            throw new IllegalArgumentException(String.format("Некорректный тип данных: %s", fieldMapping.getRdmDataType()));
        Object result = null;
        //если типы данных бд у полей совпадают, то сохраняем данные без изменений
        if (rdmType.equals(sysType)) {
            return value;
        }
        String classCastError = String.format("Ошибка при попытке преобразовать тип %s в %s значение: %s", rdmType.getText(), sysType.getText(), value);

        //Маппинг в JSONB возможен только при условии, что входные данные также имеют тип jSONB, так как иначе неизвестно как формат должен иметь json.
        if (sysType.equals(DataTypeEnum.JSONB)) {
            throw new ClassCastException(classCastError);
        }
        if (rdmType.equals(DataTypeEnum.VARCHAR)) {
            switch (sysType) {
                case INTEGER:
                    result = new BigInteger(value.toString());
                    break;
                case FLOAT:
                    result = Float.parseFloat(value.toString());
                    break;
                case BOOLEAN:
                    result = Boolean.parseBoolean(value.toString());
                    break;
                case DATE:
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
                    result = LocalDate.parse(value.toString(), formatter);
                    break;
            }
        } else if (rdmType.equals(DataTypeEnum.INTEGER)) {
            switch (sysType) {
                case VARCHAR:
                    result = value.toString();
                    break;
                case FLOAT:
                    result = Float.parseFloat(value.toString());
                    break;
                case BOOLEAN:
                case DATE:
                    throw new ClassCastException(classCastError);
            }
        } else if (rdmType.equals(DataTypeEnum.BOOLEAN) || rdmType.equals(DataTypeEnum.FLOAT)) {
            if (sysType.equals(DataTypeEnum.VARCHAR)) {
                result = value.toString();
            } else {
                throw new ClassCastException(classCastError);
            }
        } else if (rdmType.equals(DataTypeEnum.DATE)) {
            if (sysType.equals(DataTypeEnum.VARCHAR)) {
                if (value instanceof Date) {
                    result = FastDateFormat.getInstance(DATE_FORMAT).format(value);
                } else if (value instanceof LocalDate || value instanceof LocalDateTime) {
                    result = DateTimeFormatter.ofPattern(DATE_FORMAT).format((Temporal) value);
                } else {
                    throw new ClassCastException(classCastError);
                }
            } else {
                throw new ClassCastException(classCastError);
            }
        }
        return result;
    }

}
