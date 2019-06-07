package ru.inovus.ms.rdm.util;

import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.ReferenceFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.RefBookDataDiff;
import ru.inovus.ms.rdm.model.RefBookRowValue;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.compare.ComparableField;
import ru.inovus.ms.rdm.model.compare.ComparableFieldValue;
import ru.inovus.ms.rdm.model.compare.ComparableRow;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class ComparableUtils {

    private ComparableUtils() {
    }

    public static DiffStatusEnum getStrongestStatus(DiffStatusEnum status1, DiffStatusEnum status2) {
        if (status1 == DiffStatusEnum.DELETED || status2 == DiffStatusEnum.DELETED)
            return DiffStatusEnum.DELETED;
        if (status1 == DiffStatusEnum.INSERTED || status2 == DiffStatusEnum.INSERTED)
            return DiffStatusEnum.INSERTED;
        if (status1 == DiffStatusEnum.UPDATED || status2 == DiffStatusEnum.UPDATED)
            return DiffStatusEnum.UPDATED;
        return null;
    }

    /**
     * В списке diff-записей #diffRowValues ищется запись, которая соответствует строке #rowValue
     * на основании набора первичных ключей primaries.
     *
     * @param primaries     список первичных атрибутов для идентификации записи
     * @param rowValue      запись, для которой ведется поиск в полученном списке записей
     * @param diffRowValues список diff-записей, среди которых ведется поиск
     * @return Найденная diff-запись об изменениях либо null
     */
    public static DiffRowValue findDiffRowValue(List<Structure.Attribute> primaries, RowValue rowValue,
                                                List<DiffRowValue> diffRowValues) {
        return diffRowValues
                .stream()
                .filter(diffRow ->
                        primaries.stream().allMatch(primary -> {
                            DiffFieldValue diffFieldValue = diffRow.getDiffFieldValue(primary.getCode());
                            return diffFieldValue != null &&
                                    rowValue.getFieldValue(primary.getCode()).getValue()
                                            .equals(
                                                    DiffStatusEnum.DELETED.equals(diffFieldValue.getStatus())
                                                            ? diffFieldValue.getOldValue()
                                                            : diffFieldValue.getNewValue()
                                            );
                        })
                )
                .findFirst()
                .orElse(null);
    }

    private static boolean isRefBookRowValue(List<Structure.Attribute> primaries, Structure.Attribute refAttribute,
                                             DiffRowValue diffRowValue, RefBookRowValue rowValue) {

//        на данный момент может быть только: 1 поле -> 1 первичный ключ (ссылка на составной ключ невозможна)
        DiffFieldValue diffFieldValue = diffRowValue.getDiffFieldValue(primaries.get(0).getCode());
        return Objects.equals(
                castRefValue(rowValue.getFieldValue(refAttribute.getCode()), primaries.get(0).getType()),
                DiffStatusEnum.DELETED.equals(diffRowValue.getStatus())
                        ? diffFieldValue.getOldValue()
                        : diffFieldValue.getNewValue()
        );
    }

    /**
     * В списке записей #rowValues ищется первая запись,
     * которая соответствует строке об изменениях #diffRowValue
     * на основании набора первичных ключей primaries.
     *
     * @param primaries    список первичных атрибутов для идентификации записи
     * @param diffRowValue diff-запись, для которой ведется поиск в полученном списке записей
     * @param rowValues    список записей, среди которых ведется поиск
     * @return Найденная запись либо null
     */
    public static RefBookRowValue findRefBookRowValue(List<Structure.Attribute> primaries, Structure.Attribute refAttribute,
                                                      DiffRowValue diffRowValue, List<RefBookRowValue> rowValues) {
        return rowValues
                .stream()
                .filter(rowValue -> isRefBookRowValue(primaries, refAttribute, diffRowValue, rowValue))
                .findFirst()
                .orElse(null);
    }

    /**
     * В списке записей #rowValues ищутся записи,
     * которые соответствует строке об изменениях #diffRowValue
     * на основании набора первичных ключей primaries.
     *
     * @param primaries    список первичных атрибутов для идентификации записи
     * @param diffRowValue diff-запись, для которой ведется поиск в полученном списке записей
     * @param rowValues    список записей, среди которых ведется поиск
     * @return Список найденных записей
     */
    public static List<RefBookRowValue> findRefBookRowValues(List<Structure.Attribute> primaries, Structure.Attribute refAttribute,
                                                             DiffRowValue diffRowValue, List<RefBookRowValue> rowValues) {
        return rowValues
                .stream()
                .filter(rowValue -> isRefBookRowValue(primaries, refAttribute, diffRowValue, rowValue))
                .collect(Collectors.toList());
    }

    /**
     * В списке записей #rowValues ищется строка, которая соответствует строке #rowValue
     * на основании набора первичных ключей primaries.
     *
     * @param primaries список первичных атрибутов для идентификации записи
     * @param rowValue  запись, для которой ведется поиск соответствующей в полученном списке записей
     * @param rowValues список записей, среди которых ведется поиск
     * @return Найденная запись либо null
     */
    public static RowValue findRowValue(List<Structure.Attribute> primaries, RowValue rowValue,
                                        List<? extends RowValue> rowValues) {
        return rowValues
                .stream()
                .filter(rowValue1 ->
                        primaries.stream().allMatch(primary -> {
                            FieldValue fieldValue = rowValue.getFieldValue(primary.getCode());
                            FieldValue fieldValue1 = rowValue1.getFieldValue(primary.getCode());
                            return fieldValue != null
                                    && fieldValue1 != null
                                    && fieldValue.getValue() != null
                                    && fieldValue.getValue().equals(fieldValue1.getValue());
                        })
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * В списке записей #comparableRows ищется строка, которая соответствует строке #rowValue
     * на основании набора первичных ключей primaries.
     *
     * @param primaries      список первичных атрибутов для идентификации записи
     * @param rowValue       запись, для которой ведется поиск соответствующей
     * @param comparableRows список записей, среди которых ведется поиск
     * @return Найденная запись либо null
     */
    public static ComparableRow findComparableRow(List<Structure.Attribute> primaries, RowValue rowValue,
                                                  List<ComparableRow> comparableRows, DiffStatusEnum status) {
        return comparableRows
                .stream()
                .filter(comparableRow ->
                        primaries.stream().allMatch(primary -> {
                            ComparableFieldValue comparableValue = comparableRow.getComparableFieldValue(primary.getCode());
                            return comparableValue != null &&
                                    rowValue.getFieldValue(primary.getCode()).getValue().equals(
                                            DiffStatusEnum.DELETED.equals(status)
                                                    ? comparableValue.getOldValue()
                                                    : comparableValue.getNewValue()
                                    );
                        })
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * Для полученного набора строк заполняется множество фильтров по первичным полям
     *
     * @param refBookDataDiff информация об измененных строк, для которых необходимо создать фильтры
     * @param structure       структура версии, для определения первичных полей
     * @return Множество фильтров по первичным полям версии
     */
    public static Set<List<AttributeFilter>> createPrimaryAttributesFilters(RefBookDataDiff refBookDataDiff, Structure structure) {
        return refBookDataDiff.getRows().getContent().stream().map(row ->
                structure.getPrimary()
                        .stream()
                        .map(pk ->
                                new AttributeFilter(
                                        pk.getCode(),
                                        DiffStatusEnum.DELETED.equals(row.getStatus())
                                                ? row.getDiffFieldValue(pk.getCode()).getOldValue()
                                                : row.getDiffFieldValue(pk.getCode()).getNewValue(),
                                        pk.getType())
                        )
                        .collect(toList())
        ).collect(toSet());
    }

    /**
     * Для полученного набора строк заполняется множество фильтров по первичным полям
     *
     * @param data      множество строк, значения которых будут переведны в фильтры
     * @param structure структура версии, для определения первичных полей
     * @return Множество фильтров по первичным полям версии
     */
    public static Set<List<AttributeFilter>> createPrimaryAttributesFilters(Page<? extends RowValue> data, Structure structure) {
        return data.getContent().stream().map(row ->
                structure.getPrimary()
                        .stream()
                        .map(pk ->
                                new AttributeFilter(pk.getCode(), row.getFieldValue(pk.getCode()).getValue(), pk.getType())
                        )
                        .collect(toList())
        ).collect(toSet());
    }

    /**
     * Возвращает для двух версий общий список атрибутов со статусами.
     * Содержит неизмененные, измененные добавленные атрибуты в порядке в порядке их
     * расположения в новой структуре, удаленные атрибуты в конце списка в порядке их расположения в старой структуре.
     *
     * @param refBookDataDiff изменения для сравниваемых версий
     * @param newStructure    структура новой версии, определяет порядок полей
     * @param oldStructure    структура старой версии, определяет порядок удаленных полей в конце списка
     * @return Список атрибутов
     */
    public static List<ComparableField> createCommonComparableFieldsList(RefBookDataDiff refBookDataDiff,
                                                                         Structure newStructure, Structure oldStructure) {
        List<ComparableField> comparableFields = newStructure.getAttributes().stream().map(attribute -> {
            DiffStatusEnum fieldStatus = null;
            if (refBookDataDiff.getUpdatedAttributes().contains(attribute.getCode()))
                fieldStatus = DiffStatusEnum.UPDATED;
            if (refBookDataDiff.getNewAttributes().contains(attribute.getCode()))
                fieldStatus = DiffStatusEnum.INSERTED;
            return new ComparableField(attribute.getCode(), attribute.getName(), fieldStatus);
        }).collect(toList());

        refBookDataDiff.getOldAttributes()
                .forEach(oldAttribute ->
                        comparableFields.add(
                                new ComparableField(oldAttribute, oldStructure.getAttribute(oldAttribute).getName(),
                                        DiffStatusEnum.DELETED))
                );
        return comparableFields;
    }

    private static Object castRefValue(FieldValue fieldValue, FieldType refFieldType) {
        if (fieldValue instanceof ReferenceFieldValue) {
            Reference value = (Reference) fieldValue.getValue();
            if (refFieldType == FieldType.INTEGER) {
                return value.getValue() != null ? BigInteger.valueOf(Integer.valueOf(value.getValue())) : null;
            }
            return value.getValue();
        }
        return fieldValue.getValue();
    }

}
