package ru.i_novus.ms.rdm.impl.entity.loader;

import ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "ref_book_data_load_log", schema = "n2o_rdm_management")
public class RefBookDataLoadLogEntity implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "change_set_id", nullable = false)
    private String changeSetId;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "update_type", nullable = false)
    private RefBookDataUpdateTypeEnum updateType;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "ref_book_id")
    private Integer refBookId;

    @Column(name = "executed_date")
    private LocalDateTime executedDate;

    // Hibernate only.
    public RefBookDataLoadLogEntity() {
        // Nothing to do.
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getChangeSetId() {
        return changeSetId;
    }

    public void setChangeSetId(String changeSetId) {
        this.changeSetId = changeSetId;
    }

    public RefBookDataUpdateTypeEnum getUpdateType() {
        return updateType;
    }

    public void setUpdateType(RefBookDataUpdateTypeEnum updateType) {
        this.updateType = updateType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getRefBookId() {
        return refBookId;
    }

    public void setRefBookId(Integer refBookId) {
        this.refBookId = refBookId;
    }

    public LocalDateTime getExecutedDate() {
        return executedDate;
    }

    public void setExecutedDate(LocalDateTime executedDate) {
        this.executedDate = executedDate;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RefBookDataLoadLogEntity that = (RefBookDataLoadLogEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(changeSetId, that.changeSetId) &&
                Objects.equals(updateType, that.updateType) &&
                Objects.equals(code, that.code) &&

                Objects.equals(filePath, that.filePath) &&
                Objects.equals(fileName, that.fileName) &&

                Objects.equals(refBookId, that.refBookId) &&
                Objects.equals(executedDate, that.executedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, changeSetId, updateType, code, filePath, fileName, refBookId, executedDate);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RefBookDataLoadLogEntity{");
        sb.append("id=").append(id);
        sb.append(", changeSetId='").append(changeSetId).append('\'');
        sb.append(", updateType=").append(updateType);
        sb.append(", code='").append(code).append('\'');

        sb.append(", filePath='").append(filePath).append('\'');
        sb.append(", fileName='").append(fileName).append('\'');

        sb.append(", refBookId=").append(refBookId);
        sb.append(", executedDate=").append(executedDate);
        sb.append('}');
        return sb.toString();
    }
}