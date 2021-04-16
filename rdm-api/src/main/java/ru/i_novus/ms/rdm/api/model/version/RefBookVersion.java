package ru.i_novus.ms.rdm.api.model.version;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;

import java.time.LocalDateTime;
import java.util.Map;

@ApiModel("Версия справочника")
public class RefBookVersion {

    @ApiModelProperty("Идентификатор версии")
    private Integer id;

    @ApiModelProperty("Значение оптимистической блокировки версии")
    private Integer optLockValue;

    @ApiModelProperty("Идентификатор справочника")
    private Integer refBookId;

    @ApiModelProperty("Код справочника")
    private String code;

    @ApiModelProperty("Тип справочника")
    private RefBookType type = RefBookType.DEFAULT;

    @ApiModelProperty("Категория")
    private String category;

    @ApiModelProperty("Версия")
    private String version;

    @ApiModelProperty("Комментарий к версии")
    private String comment;

    @ApiModelProperty("Дата публикации")
    private LocalDateTime fromDate;

    @ApiModelProperty("Дата окончания действия")
    private LocalDateTime toDate;

    @ApiModelProperty("Статус версии")
    private RefBookVersionStatus status;

    @ApiModelProperty("В архиве")
    private Boolean archived;

    @ApiModelProperty("Паспорт справочника")
    private Map<String, String> passport;

    @ApiModelProperty("Структура версии")
    private Structure structure;

    @ApiModelProperty("Дата последнего изменения")
    private LocalDateTime editDate;

    public RefBookVersion() {
    }

    public RefBookVersion(RefBookVersion refBookVersion) {

        this.id = refBookVersion.getId();
        this.optLockValue = refBookVersion.getOptLockValue();
        this.refBookId = refBookVersion.getRefBookId();

        this.code = refBookVersion.getCode();
        this.type = refBookVersion.getType();
        this.category = refBookVersion.getCategory();
        this.version = refBookVersion.getVersion();
        this.comment = refBookVersion.getComment();

        this.fromDate = refBookVersion.getFromDate();
        this.toDate = refBookVersion.getToDate();
        this.status = refBookVersion.getStatus();
        this.archived = refBookVersion.getArchived();

        this.passport = refBookVersion.getPassport();
        this.structure = refBookVersion.getStructure();

        this.editDate = refBookVersion.getEditDate();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public void setOptLockValue(Integer optLockValue) {
        this.optLockValue = optLockValue;
    }

    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public RefBookType getType() {
        return type;
    }

    public void setType(RefBookType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }

    public RefBookVersionStatus getStatus() {
        return status;
    }

    public void setStatus(RefBookVersionStatus status) {
        this.status = status;
    }

    public Boolean getArchived() {
        return archived != null && archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Map<String, String> getPassport() {
        return passport;
    }

    public void setPassport(Map<String, String> passport) {
        this.passport = passport;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public LocalDateTime getEditDate() {
        return editDate;
    }

    public void setEditDate(LocalDateTime editDate) {
        this.editDate = editDate;
    }

    /**
     * Проверка статуса версии на DRAFT.
     *
     * @return Результат проверки
     */
    public boolean isDraft() {
        return RefBookVersionStatus.DRAFT.equals(status);
    }

    /**
     * Проверка отсутствия структуры.
     *
     * @return Результат проверки
     */
    public boolean hasEmptyStructure() {
        return structure == null || structure.isEmpty();
    }
}