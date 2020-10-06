package ru.i_novus.ms.rdm.api.util;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableField;
import ru.i_novus.ms.rdm.api.model.compare.ComparableFieldValue;
import ru.i_novus.ms.rdm.api.model.compare.ComparableRow;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.field.ReferenceFilterValue;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.version.AttributeFilter;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

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
     * В списке diff-записей #diffRowValues ищется запись, которая соответствует
     * строке #rowValue на основании набора первичных ключей #primaries.
     *
     * @param primaries     список первичных атрибутов для идентификации записи
     * @param rowValue      запись, для которой ведется поиск в полученном списке записей
     * @param diffRowValues список diff-записей, среди которых ведется поиск
     * @return Найденная diff-запись об изменениях либо null
     */
    public static DiffRowValue findDiffRowValue(List<Structure.Attribute> primaries, RowValue rowValue,
                                                List<DiffRowValue> diffRowValues) {
        return diffRowValues.stream()
                .filter(diffRow ->
                        primaries.stream().allMatch(primary -> {
                            DiffFieldValue diffFieldValue = diffRow.getDiffFieldValue(primary.getCode());
                            return diffFieldValue != null &&
                                    Objects.equals(rowValue.getFieldValue(primary.getCode()).getValue(),
                                            FieldValueUtils.getDiffFieldValue(diffFieldValue, diffFieldValue.getStatus()));
                        })
                )
                .findFirst().orElse(null);
    }

    /**
     * Проверяет, что запись об изменениях #diffRowValue соответствует записи #rowValue
     * на основании набора первичных ключей #primaries и ссылочного атрибута #refAttribute.
     *
     * Возвращает true, если значение ссылочного атрибута из #rowValue равно значению первичного поля из #diffRowValue.
     *
     * @param primaries    список первичных атрибутов версии, НА которую ссылаются
     *                     (для получения данных из записи diffRowValue)
     * @param refAttribute ссылочный атрибут версии, которая ссылается
     *                     (для получения данных из записи rowValue)
     * @param diffRowValue diff-запись об изменениях в версии, на которую ссылаемся
     * @param rowValue     запись, которая ссылается
     * @return true, если #diffRowValue соответсвует #rowValue; иначе false
     */
    private static boolean isRefBookRowValue(List<Structure.Attribute> primaries, Structure.Attribute refAttribute,
                                             DiffRowValue diffRowValue, RefBookRowValue rowValue) {

//        на данный момент может быть только: 1 поле -> 1 первичный ключ (ссылка на составной ключ невозможна)
        DiffFieldValue diffFieldValue = diffRowValue.getDiffFieldValue(primaries.get(0).getCode());
        return Objects.equals(FieldValueUtils.getDiffFieldValue(diffFieldValue, diffRowValue.getStatus()),
                FieldValueUtils.castFieldValue(rowValue.getFieldValue(refAttribute.getCode()), primaries.get(0).getType())
        );
    }

    /**
     * В списке записей #rowValues ищется первая запись,
     * которая соответствует строке об изменениях #diffRowValue
     * на основании набора первичных ключей #primaries.
     *
     * @param primaries    список первичных атрибутов для идентификации записи
     * @param refAttribute ссылочный атрибут версии, которая ссылается
     *                     (для получения данных из записи rowValue)
     * @param diffRowValue diff-запись, для которой ведется поиск в полученном списке записей
     * @param rowValues    список записей, среди которых ведется поиск
     * @return Найденная запись либо null
     */
    public static RefBookRowValue findRefBookRowValue(List<Structure.Attribute> primaries, Structure.Attribute refAttribute,
                                                      DiffRowValue diffRowValue, List<RefBookRowValue> rowValues) {
        return rowValues.stream()
                .filter(rowValue -> isRefBookRowValue(primaries, refAttribute, diffRowValue, rowValue))
                .findFirst().orElse(null);
    }

    /**
     * В списке записей #rowValues ищутся записи,
     * которые соответствует строке об изменениях #diffRowValue
     * на основании набора первичных ключей #primaries.
     *
     * @param primaries    список первичных атрибутов для идентификации записи
     * @param refAttribute ссылочный атрибут версии, которая ссылается
     *                     (для получения данных из записи rowValue)
     * @param diffRowValue diff-запись, для которой ведется поиск в полученном списке записей
     * @param rowValues    список записей, среди которых ведется поиск
     * @return Список найденных записей
     */
    public static List<RefBookRowValue> findRefBookRowValues(List<Structure.Attribute> primaries, Structure.Attribute refAttribute,
                                                             DiffRowValue diffRowValue, List<RefBookRowValue> rowValues) {
        return rowValues.stream()
                .filter(rowValue -> isRefBookRowValue(primaries, refAttribute, diffRowValue, rowValue))
                .collect(Collectors.toList());
    }

    /**
     * В списке записей #rowValues ищется строка, которая соответствует
     * записи #rowValue на основании набора первичных ключей #primaries.
     *
     * @param primaries список первичных атрибутов для идентификации записи
     * @param rowValue  запись, для которой ведется поиск соответствующей в полученном списке записей
     * @param rowValues список записей, среди которых ведется поиск
     * @return Найденная запись либо null
     */
    public static RowValue findRowValue(List<Structure.Attribute> primaries, RowValue rowValue,
                                        List<? extends RowValue> rowValues) {
        return rowValues.stream()
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
                .findFirst().orElse(null);
    }

    /**
     * В списке записей #comparableRows ищется строка, которая соответствует
     * записи #rowValue на основании набора первичных ключей #primaries.
     *
     * @param primaries      список первичных атрибутов для идентификации записи
     * @param rowValue       запись, для которой ведется поиск соответствующей
     * @param comparableRows список записей, среди которых ведется поиск
     * @param status         статус записи для получения нужного значения
     * @return Найденная запись либо null
     */
    public static ComparableRow findComparableRow(List<Structure.Attribute> primaries, RowValue rowValue,
                                                  List<ComparableRow> comparableRows, DiffStatusEnum status) {
        return comparableRows.stream()
                .filter(comparableRow ->
                        primaries.stream().allMatch(primary -> {
                            ComparableFieldValue comparableValue = comparableRow.getComparableFieldValue(primary.getCode());
                            return comparableValue != null &&
                                    Objects.equals(FieldValueUtils.getCompareFieldValue(comparableValue, status),
                                            rowValue.getFieldValue(primary.getCode()).getValue());
                        })
                )
                .findFirst().orElse(null);
    }

    /**
     * Для полученного набора строк заполняется множество фильтров по первичным полям.
     *
     * @param refBookDataDiff информация об измененных строк, для которых необходимо создать фильтры
     * @param structure       структура версии, для определения первичных полей
     * @return Множество фильтров по первичным полям версии
     */
    public static Set<List<AttributeFilter>> createPrimaryAttributesFilters(RefBookDataDiff refBookDataDiff, Structure structure) {

        return refBookDataDiff.getRows().getContent().stream()
                .map(row ->
                        structure.getPrimaries().stream()
                                .map(pk ->
                                        new AttributeFilter(
                                                pk.getCode(),
                                                FieldValueUtils.getDiffFieldValue(row.getDiffFieldValue(pk.getCode()), row.getStatus()),
                                                pk.getType())
                                )
                                .collect(toList())
                ).collect(toSet());
    }

    /**
     * Для полученного набора строк заполняется множество фильтров по первичным полям.
     *
     * @param data      множество строк, значения которых будут переведны в фильтры
     * @param structure структура версии, для определения первичных полей
     * @return Множество фильтров по первичным полям версии
     */
    public static Set<List<AttributeFilter>> createPrimaryAttributesFilters(Page<? extends RowValue> data, Structure structure) {

        return data.getContent().stream()
                .map(row ->
                        structure.getPrimaries().stream()
                                .map(pk ->
                                        new AttributeFilter(pk.getCode(), row.getFieldValue(pk.getCode()).getValue(), pk.getType())
                                )
                                .collect(toList())
                ).collect(toSet());
    }

    /**
     * Возвращает для двух версий общий список атрибутов со статусами.
     * Содержит изменённые и добавленные атрибуты в порядке их расположения в новой структуре,
     * удалённые атрибуты в конце списка в порядке их расположения в старой структуре.
     *
     * @param refBookDataDiff изменения для сравниваемых версий
     * @param newStructure    структура новой версии, определяет порядок полей
     * @param oldStructure    структура старой версии, определяет порядок удаленных полей в конце списка
     * @return Список атрибутов
     */
    public static List<ComparableField> createCommonComparableFieldsList(RefBookDataDiff refBookDataDiff,
                                                                         Structure newStructure, Structure oldStructure) {
        List<ComparableField> comparableFields = newStructure.getAttributes().stream()
                .map(attribute -> {
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

    /**
     * Проверка на наличие изменения структуры.
     *
     * @param diff различие в структурах версий
     * @return Наличие изменения структуры
     */
    public static boolean isRefBookAltered(StructureDiff diff) {
        return !diff.getInserted().isEmpty()
                || !diff.getDeleted().isEmpty()
                || diff.getUpdated().stream().anyMatch(
                updated -> !updated.getNewAttribute().getType()
                        .equals(updated.getOldAttribute().getType()));
    }

    /**
     * Поиск записи о различии по ссылочному значению.
     *
     * @param filterValue   значение ссылочного поля
     * @param diffRowValues список различий
     * @return Запись о различии
     */
    public static DiffRowValue findDiffRowValue(ReferenceFilterValue filterValue, List<DiffRowValue> diffRowValues) {

        return diffRowValues.stream()
                .filter(diffRowValue -> {
                    DiffFieldValue diffFieldValue = diffRowValue.getDiffFieldValue(filterValue.getAttribute().getCode());
                    return Objects.nonNull(diffFieldValue)
                            && Objects.equals(FieldValueUtils.getDiffFieldValue(diffFieldValue, diffRowValue.getStatus()),
                            FieldValueUtils.castFieldValue(filterValue.getReferenceValue(), filterValue.getAttribute().getType()));
                })
                .findFirst().orElse(null);
    }
}
