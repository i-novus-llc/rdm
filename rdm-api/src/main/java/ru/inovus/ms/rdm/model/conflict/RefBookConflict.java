package ru.inovus.ms.rdm.model.conflict;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.time.LocalDateTime;

@ApiModel("Конфликт")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefBookConflict {

    @ApiModelProperty("Идентификатор версии справочника со ссылками")
    private Integer referrerVersionId;

    @ApiModelProperty("Идентификатор версии опубликованного справочника")
    private Integer publishedVersionId;

    @ApiModelProperty("Системный идентификатор записи с конфликтом")
    private Long refRecordId;

    @ApiModelProperty("Название поля-ссылки с конфликтом по отображаемому значению")
    private String refFieldCode;

    @ApiModelProperty("Тип конфликта")
    private ConflictType conflictType;

    @ApiModelProperty("Дата создания записи")
    private LocalDateTime creationDate;

    public RefBookConflict() {
    }

    public RefBookConflict(Integer referrerVersionId, Integer publishedVersionId,
                           Long refRecordId, String refFieldCode, ConflictType conflictType,
                           LocalDateTime creationDate) {
        this.referrerVersionId = referrerVersionId;
        this.publishedVersionId = publishedVersionId;
        this.refRecordId = refRecordId;
        this.refFieldCode = refFieldCode;
        this.conflictType = conflictType;
        this.creationDate = creationDate;
    }

    public Integer getReferrerVersionId() {
        return referrerVersionId;
    }

    public void setReferrerVersionId(Integer referrerVersionId) {
        this.referrerVersionId = referrerVersionId;
    }

    public Integer getPublishedVersionId() {
        return publishedVersionId;
    }

    public void setPublishedVersionId(Integer publishedVersionId) {
        this.publishedVersionId = publishedVersionId;
    }

    public Long getRefRecordId() {
        return refRecordId;
    }

    public void setRefRecordId(Long refRecordId) {
        this.refRecordId = refRecordId;
    }

    public String getRefFieldCode() {
        return refFieldCode;
    }

    public void setRefFieldCode(String refFieldCode) {
        this.refFieldCode = refFieldCode;
    }

    public ConflictType getConflictType() {
        return conflictType;
    }

    public void setConflictType(ConflictType conflictType) {
        this.conflictType = conflictType;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }


    /**
     * Проверка типа на CLEANED.
     *
     * @return Результат проверки
     */
    public boolean isCleaned() {
        return ConflictType.CLEANED.equals(getConflictType());
    }
}
