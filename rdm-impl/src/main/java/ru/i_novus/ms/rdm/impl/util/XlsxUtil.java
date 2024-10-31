package ru.i_novus.ms.rdm.impl.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.ExcelStyleDateFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import static ru.i_novus.ms.rdm.api.util.StringUtils.isEmpty;

/**
 * Класс для работы с Xlsx.
 */
public final class XlsxUtil {

    /**
     * Формат дат в xlsx.
     */
    public static final String XLSX_DATE_FORMAT = "dd.MM.yyyy";

    private XlsxUtil() {
        // Nothing to do.
    }

    /**
     * Добавление строки в конец указанной страницы.
     *
     * @param sheet страница
     * @return Добавленная строка
     */
    public static Row createNextRow(Sheet sheet) {
        return sheet.createRow(sheet.getLastRowNum() + 1);
    }

    /**
     * Получение строкового значения ячейки.
     *
     * @param cell ячейка
     * @return Строковое значение
     */
    public static String getStringCellValue(Cell cell) {
        return toValueOrNull(cell.getStringCellValue());
    }

    /**
     * Получение значения ячейки в строковом виде.
     *
     * @param cell ячейка
     * @return Значение ячейки
     */
    public static String getCellValue(Cell cell) {
        return getCellValue(cell, new ExcelStyleDateFormatter(XLSX_DATE_FORMAT));
    }

    /**
     * Получение значения ячейки в строковом виде с учётом формата.
     * <p>
     * Для формул используется тип кэшированного значения - результата формулы.
     *
     * @param cell          ячейка
     * @param dateFormatter формат даты
     * @return Значение ячейки
     */
    public static String getCellValue(Cell cell, ExcelStyleDateFormatter dateFormatter) {

        final CellType type = cell.getCellType();
        final CellType cellType = type.equals(CellType.FORMULA) ? cell.getCachedFormulaResultType() : type;
        return toValueOrNull(getCellValue(cell, cellType, dateFormatter));
    }

    /**
     * Получение значения ячейки в строковом виде с учётом типа и формата.
     *
     * @param cell          ячейка
     * @param cellType      тип ячейки
     * @param dateFormatter формат даты
     * @return Значение ячейки
     */
    public static String getCellValue(Cell cell, CellType cellType,
                                      ExcelStyleDateFormatter dateFormatter) {
        return switch (cellType) {
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? dateFormatter.format(cell.getDateCellValue())
                    : getNumericCellValue(cell);
            case STRING -> cell.getRichStringCellValue().getString();
            case BOOLEAN -> cell.getBooleanCellValue() ? "TRUE" : "FALSE";
            default -> cell.getCellFormula();
        };
    }

    /**
     * Получение числового значения ячейки в строковом виде.
     *
     * @param cell ячейка
     * @return Значение ячейки
     */
    public static String getNumericCellValue(Cell cell) {

        // dataFormatter.getFormattedNumberString недоступен.
        final String value = toNumeric(cell.getStringCellValue());
        return !isEmpty(value) ? value : String.valueOf(cell.getNumericCellValue());
    }

    /**
     * Преобразование строки со значением ячейки в строку в формате, используемом в rdm.
     *
     * @param value значение ячейки
     * @return Значение ячейки в rdm-формате
     */
    public static String toNumeric(String value) {

        return value == null
                ? null
                : value.replace(',', '.')
                        .replace("\"", "");
    }

    /**
     * Получение значения или null при его отсутствии.
     *
     * @param value строка со значением
     * @return Значение или null
     */
    public static String toValueOrNull(String value) {
        return !isEmpty(value) ? value : null;
    }
}
