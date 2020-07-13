package ru.i_novus.ms.rdm.api.model.conflict;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;

import javax.ws.rs.QueryParam;
import java.io.Serializable;
import java.util.List;

@ApiModel(value = "Вычисленный конфликт версии, которая ссылаются")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Conflict implements Serializable {

    @ApiModelProperty("Код атрибута")
    @QueryParam("refAttributeCode")
    private String refAttributeCode;

    @ApiModelProperty("Тип конфликта")
    @QueryParam("conflictType")
    private ConflictType conflictType;

    @ApiModelProperty("Список первичных ключей")
    @QueryParam("primaryValues")
    private List<FieldValue> primaryValues;

    @SuppressWarnings("unused")
    public Conflict() {
    }

    public Conflict(String refAttributeCode, ConflictType conflictType, List<FieldValue> primaryValues) {
        this.refAttributeCode = refAttributeCode;
        this.conflictType = conflictType;
        this.primaryValues = primaryValues;
    }

    public String getRefAttributeCode() {
        return refAttributeCode;
    }

    public void setRefAttributeCode(String refAttributeCode) {
        this.refAttributeCode = refAttributeCode;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public void setConflictType(ConflictType conflictType) {
        this.conflictType = conflictType;
    }

    public List<FieldValue> getPrimaryValues() {
        return primaryValues;
    }

    public void setPrimaryValues(List<FieldValue> primaryValues) {
        this.primaryValues = primaryValues;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(refAttributeCode) || CollectionUtils.isEmpty(primaryValues);
    }

    /**
     * Проверка типа на UPDATED.
     *
     * @return Результат проверки
     */
    public boolean isUpdated() {
        return ConflictType.UPDATED.equals(getConflictType());
    }
}
