package ru.inovus.ms.rdm.impl.entity;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import ru.inovus.ms.rdm.api.enumeration.RefBookOperation;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.util.TimeUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "ref_book_version", schema = "n2o_rdm_management")
@TypeDef(name = "structure", typeClass = StructureType.class)
public class RefBookVersionEntity implements Serializable {

    private static final String DRAFT_VERSION = "0.0";

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    @Column(name = "opt_lock_value", nullable = false)
    @SuppressWarnings("unused")
    private Integer optLockValue;

    @ManyToOne
    @JoinColumn(name = "ref_book_id", nullable = false)
    private RefBookEntity refBook;

    @ManyToOne
    @JoinColumn(name = "ref_book_id", referencedColumnName = "ref_book_id", insertable = false, updatable = false)
    private RefBookOperationEntity refBookOperation;

    @Column(name = "structure", columnDefinition = "json")
    @Type(type = "structure")
    private Structure structure;

    @Column(name = "storage_code")
    private String storageCode;

    @Column(name = "version")
    private String version;

    @Column(name = "comment")
    private String comment;

    @Column(name = "status", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private RefBookVersionStatus status;

    @Column(name = "from_date")
    private LocalDateTime fromDate;

    @Column(name = "to_date")
    private LocalDateTime toDate;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "last_action_date")
    private LocalDateTime lastActionDate;

    @OneToMany(mappedBy="version", cascade = CascadeType.ALL)
    private List<PassportValueEntity> passportValues;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOptLockValue() {
        return optLockValue;
    }

    public RefBookEntity getRefBook() {
        return refBook;
    }

    public void setRefBook(RefBookEntity refBook) {
        this.refBook = refBook;
    }

    public RefBookOperationEntity getRefBookOperation() {
        return refBookOperation;
    }

    public void setRefBookOperation(RefBookOperationEntity refBookOperation) {
        this.refBookOperation = refBookOperation;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
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

    public RefBookVersionStatus getStatus() {
        return status;
    }

    public void setStatus(RefBookVersionStatus status) {
        this.status = status;
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

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getLastActionDate() {
        return lastActionDate;
    }

    public void setLastActionDate(LocalDateTime lastActionDate) {
        this.lastActionDate = lastActionDate;
    }

    public String getStorageCode() {
        return storageCode;
    }

    public void setStorageCode(String storageCode) {
        this.storageCode = storageCode;
    }

    public List<PassportValueEntity> getPassportValues() {
        return passportValues;
    }

    public void setPassportValues(List<PassportValueEntity> passportValues) {
        passportValues.forEach(v -> v.setVersion(this));
        this.passportValues = passportValues;
    }

    /**
     * Проверка на операцию, выполняемую над справочником.
     *
     * @param operation операция
     * @return Результат проверки
     */
    public boolean isOperation(RefBookOperation operation) {
        return refBookOperation != null && operation.equals(refBookOperation.getOperation());
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

    /**
     * Формирование модели черновика.
     *
     * @return Модель черновика
     */
    public Draft toDraft() {
        return new Draft(getId(), getStorageCode(), getOptLockValue());
    }

    /**
     * Получение номера версии.
     *
     * @return Номер версии
     */
    public String getVersionNumber() {
        return isDraft() ? DRAFT_VERSION : getVersion();
    }

    /**
     * Получение значения паспорта по атрибуту.
     *
     * @param passportAttribute паспортный атрибут
     * @return Значение паспорта
     */
    public PassportValueEntity getPassportValue(PassportAttributeEntity passportAttribute) {

        if (getPassportValues() == null)
            return null;

        return getPassportValues().stream()
                .filter(value -> value.getAttribute().equals(passportAttribute))
                .findFirst().orElse(null);
    }

    /**
     * Преобразование паспорта справочника в набор строк.
     *
     * @return Паспорт справочника в виде набора строк
     */
    public Map<String, String> toPassport() {

        if (getPassportValues() == null)
            return null;

        return getPassportValues().stream()
                .filter(v -> v.getValue() != null)
                .sorted((v1, v2) -> {
                    if (v1.getAttribute().getPosition() == null || v2.getAttribute().getPosition() == null)
                        return 0;

                    return v1.getAttribute().getPosition() - v2.getAttribute().getPosition();
                })
                .collect(Collectors.toMap(
                        v -> v.getAttribute().getCode(),
                        PassportValueEntity::getValue,
                        (e1, e2) -> e2,
                        LinkedHashMap::new));
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = TimeUtils.now();

        if (creationDate == null)
            creationDate = now;

        if (lastActionDate == null)
            lastActionDate = now;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RefBookVersionEntity that = (RefBookVersionEntity) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(optLockValue, that.optLockValue) &&
                Objects.equals(refBook, that.refBook) &&

                Objects.equals(structure, that.structure) &&
                Objects.equals(storageCode, that.storageCode) &&
                Objects.equals(version, that.version) &&
                Objects.equals(comment, that.comment) &&

                Objects.equals(status, that.status) &&
                Objects.equals(fromDate, that.fromDate) &&
                Objects.equals(toDate, that.toDate) &&
                Objects.equals(creationDate, that.creationDate) &&
                Objects.equals(lastActionDate, that.lastActionDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, optLockValue, refBook,
                structure, storageCode, version, comment,
                status, fromDate, toDate, creationDate, lastActionDate);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RefBookVersionEntity{");
        sb.append("id=").append(id);
        sb.append(", optLockValue=").append(optLockValue);
        sb.append(", refBook=").append(refBook);

        sb.append(", structure=").append(structure);
        sb.append(", storageCode='").append(storageCode).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", comment='").append(comment).append('\'');

        sb.append(", status=").append(status);
        sb.append(", fromDate=").append(fromDate);
        sb.append(", toDate=").append(toDate);
        sb.append(", creationDate=").append(creationDate);
        sb.append(", lastActionDate=").append(lastActionDate);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Преобразование набора строк в паспорт справочника.
     *
     * @param passport      набор значений-строк
     * @param allValues     признак преобразования всех значений:
     *                      если false, то преобразуются только не-null значения
     * @param versionEntity версия, указываемая в паспортных данных
     * @return Паспорт справочника
     */
    public static List<PassportValueEntity> stringPassportToValues(Map<String, String> passport,
                                                                   boolean allValues,
                                                                   RefBookVersionEntity versionEntity) {
        return passport.entrySet().stream()
                .filter(e -> allValues || e.getValue() != null)
                .map(e -> new PassportValueEntity(new PassportAttributeEntity(e.getKey()), e.getValue(), versionEntity))
                .collect(Collectors.toList());
    }

    /**
     * Преобразование набора значений в паспорт справочника.
     *
     * @param passport      набор значений
     * @param allValues     признак преобразования всех значений:
     *                      если false, то преобразуются только не-null значения
     * @param versionEntity версия, указываемая в паспортных данных
     * @return Паспорт справочника
     */
    public static List<PassportValueEntity> objectPassportToValues(Map<String, Object> passport,
                                                                   boolean allValues,
                                                                   RefBookVersionEntity versionEntity) {
        return passport.entrySet().stream()
                .filter(e -> allValues || e.getValue() != null)
                .map(e -> new PassportValueEntity(new PassportAttributeEntity(e.getKey()), (String) e.getValue(), versionEntity))
                .collect(Collectors.toList());
    }
}