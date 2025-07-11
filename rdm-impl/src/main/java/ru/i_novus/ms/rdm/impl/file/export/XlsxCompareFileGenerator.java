package ru.i_novus.ms.rdm.impl.file.export;

import net.n2oapp.platform.i18n.UserException;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableRow;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.PassportDiff;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.PageIterator;
import ru.i_novus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.i_novus.ms.rdm.impl.repository.PassportAttributeRepository;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.i_novus.ms.rdm.impl.util.XlsxUtil.XLSX_DATE_FORMAT;
import static ru.i_novus.ms.rdm.impl.util.XlsxUtil.createNextRow;

/**
 * Created by znurgaliev on 26.09.2018.
 */
class XlsxCompareFileGenerator implements FileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(XlsxCompareFileGenerator.class);

    private static final Color RED_FONT_COLOR = Color.RED;
    private static final Color BLUE_FONT_COLOR = new Color(31, 78, 120);
    private static final Color GREEN_FONT_COLOR = new Color(0, 128, 0);
    private static final Color RED_CELL_COLOR = new Color(255, 204, 204);
    private static final Color BLUE_CELL_COLOR = new Color(189, 215, 238);
    private static final Color GREEN_CELL_COLOR = new Color(226, 239, 218);
    private static final DefaultIndexedColorMap XSSF_COLOR_MAP = new DefaultIndexedColorMap();

    private final CompareService compareService;
    private final VersionService versionService;
    private final PassportAttributeRepository passportAttributeRepository;

    private final Integer oldVersionId;
    private final Integer newVersionId;

    private final SXSSFWorkbook wb = new SXSSFWorkbook(500);
    private final CellStyle defaultStyle;
    private final CellStyle headStyle;
    private final CellStyle headInsertStyle;
    private final CellStyle headDeleteStyle;
    private final CellStyle insertStyle;
    private final CellStyle updNewStyle;
    private final CellStyle updOldStyle;
    private final CellStyle deleteStyle;
    private final CellStyle dateStyle;
    private final CellStyle insertDateStyle;
    private final CellStyle updNewDateStyle;
    private final CellStyle updOldDateStyle;
    private final CellStyle deleteDateStyle;

    private RefBookVersion oldVersion;
    private RefBookVersion newVersion;

    private final Map<String, Integer> structureColumnIndexes = new HashMap<>();
    private final Map<String, Integer> dataColumnIndexes = new HashMap<>();

    public XlsxCompareFileGenerator(Integer oldVersionId,
                                    Integer newVersionId,
                                    CompareService compareService,
                                    VersionService versionService,
                                    PassportAttributeRepository passportAttributeRepository) {
        this.oldVersionId = oldVersionId;
        this.newVersionId = newVersionId;

        this.compareService = compareService;
        this.versionService = versionService;
        this.passportAttributeRepository = passportAttributeRepository;

        defaultStyle = createCellStyle(null, null);
        headStyle = createCellStyle(createFont(null, true, false), null);
        headInsertStyle = createCellStyle(createFont(GREEN_FONT_COLOR, true, false), GREEN_CELL_COLOR);
        headDeleteStyle = createCellStyle(createFont(RED_FONT_COLOR, true, false), RED_CELL_COLOR);
        insertStyle = createCellStyle(createFont(GREEN_FONT_COLOR, false, false), GREEN_CELL_COLOR);
        updNewStyle = createCellStyle(createFont(BLUE_FONT_COLOR, false, false), BLUE_CELL_COLOR);
        updNewStyle.setBorderBottom(BorderStyle.NONE);
        updOldStyle = createCellStyle(createFont(RED_FONT_COLOR, false, true), BLUE_CELL_COLOR);
        updOldStyle.setBorderTop(BorderStyle.NONE);
        deleteStyle = createCellStyle(createFont(RED_FONT_COLOR, false, false), RED_CELL_COLOR);
        dateStyle = createDateCellStyle(null, null);
        insertDateStyle = createDateCellStyle(createFont(GREEN_FONT_COLOR, false, false), GREEN_CELL_COLOR);
        updNewDateStyle = createDateCellStyle(createFont(BLUE_FONT_COLOR, false, false), BLUE_CELL_COLOR);
        updOldDateStyle = createDateCellStyle(createFont(RED_FONT_COLOR, false, true), BLUE_CELL_COLOR);
        deleteDateStyle = createDateCellStyle(createFont(RED_FONT_COLOR, false, false), RED_CELL_COLOR);
    }

    private XSSFCellStyle createCellStyle(Font font, Color background) {

        final XSSFCellStyle cellStyle = (XSSFCellStyle) wb.createCellStyle();
        if (font != null) {
            cellStyle.setFont(font);
        }

        if (background != null) {
            cellStyle.setFillForegroundColor(createXSSFColor(background)); // to-do: Check FG vs BG!
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);

        return cellStyle;
    }

    private XSSFCellStyle createDateCellStyle(Font font, Color background) {

        final XSSFCellStyle cellStyle = createCellStyle(font, background);
        cellStyle.setDataFormat(wb.createDataFormat().getFormat(XLSX_DATE_FORMAT));

        return cellStyle;
    }

    private Font createFont(Color color, boolean bold, boolean strikeout) {

        final XSSFFont font = (XSSFFont) wb.createFont();
        font.setFontHeightInPoints((short) 11);
        if (color != null) {
            font.setColor(createXSSFColor(color));
        }
        font.setStrikeout(strikeout);
        font.setBold(bold);

        return font;
    }

    private XSSFColor createXSSFColor(Color color) {
        return new XSSFColor(color, XSSF_COLOR_MAP);
    }

    @Override
    public void generate(OutputStream outputStream) {

        oldVersion = versionService.getById(oldVersionId);
        newVersion = versionService.getById(newVersionId);

        addPassportCompare();
        addStructureCompare();
        addDataCompare();
        try {
            wb.write(outputStream);

        } catch (IOException e) {
            throw new RdmException("cannot.create.file", e);
        }
    }

    @Override
    public void close() throws IOException {

        wb.close();
        wb.dispose();
    }

    private void addPassportCompare() {

        List<PassportAttributeEntity> attributes = passportAttributeRepository.findAllByComparableIsTrueOrderByPositionAsc();
        PassportDiff passportDiff = compareService.comparePassports(oldVersion.getId(), newVersion.getId());
        Map<String, String> newPassport = newVersion.getPassport();
        Map<String, XlsxComparedCell> compared = new LinkedHashMap<>();
        compared.putAll(newPassport.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> new XlsxComparedCell(null, e.getValue(), null)
        )));
        compared.putAll(passportDiff.getPassportAttributeDiffs().stream().collect(Collectors.toMap(
                diff -> diff.getPassportAttribute().getCode(),
                diff -> new XlsxComparedCell(
                        diff.getOldValue(),
                        diff.getNewValue(),
                        calculateEditStatus(diff.getOldValue(), diff.getNewValue())
                )
        )));

        SXSSFSheet sheet = wb.createSheet("Паспорт справочника");
        sheet.trackColumnForAutoSizing(0);
        createStatusCells(sheet);
        attributes.forEach(attribute -> {
            Cell headCell = createNextRow(sheet).createCell(0);
            headCell.setCellStyle(headStyle);
            headCell.setCellValue(attribute.getName());
            Cell valueCell = createNextRow(sheet).createCell(0);
            XlsxComparedCell diffValue = compared.get(attribute.getCode());
            if (diffValue != null && DiffStatusEnum.UPDATED.equals(diffValue.getStatus())) {
                if (Objects.equals(diffValue.getOldValue(), diffValue.getNewValue())) {
                    sheet.addMergedRegion(new CellRangeAddress(
                            valueCell.getRowIndex(), valueCell.getRowIndex() + 1,
                            valueCell.getColumnIndex(), valueCell.getColumnIndex()));
                } else getOrCreateRow(sheet, valueCell.getRowIndex() + 1);
            }
            insertCellDiffValue(valueCell, diffValue);
        });
        sheet.autoSizeColumn(0, true);

    }

    private DiffStatusEnum calculateEditStatus(Object oldValue, Object newValue) {

        if (oldValue == null) {
            return DiffStatusEnum.INSERTED;

        } else if (newValue == null) {
            return DiffStatusEnum.DELETED;

        } else {
            return DiffStatusEnum.UPDATED;
        }
    }

    private void addStructureCompare() {

        SXSSFSheet sheet = wb.createSheet("Структура справочника");
        createStatusCells(sheet);
        Row headRow = createStructureHead(sheet);
        sheet.trackAllColumnsForAutoSizing();

        Structure newStructure = newVersion.getStructure();
        StructureDiff structureDiff = compareService.compareStructures(oldVersion.getId(), newVersion.getId());

        newStructure.getAttributes().stream()
                .map(attribute -> structureDiff.getInserted().stream()
                        .filter(diff -> Objects.equals(diff.getNewAttribute().getCode(), attribute.getCode()))
                        .map(diff -> createComparedRow(diff.getOldAttribute(), diff.getNewAttribute(), DiffStatusEnum.INSERTED))
                        .findAny().orElse(structureDiff.getUpdated().stream()
                                .filter(diff -> Objects.equals(diff.getNewAttribute().getCode(), attribute.getCode()))
                                .map(diff -> createComparedRow(diff.getOldAttribute(), diff.getNewAttribute(), DiffStatusEnum.UPDATED))
                                .findAny().orElse(
                                        createComparedRow(null, attribute, null))))
                .forEach(rowDiff -> insertRowDiff(rowDiff, sheet, structureColumnIndexes));

        structureDiff.getDeleted().stream()
                .map(diff -> createComparedRow(diff.getOldAttribute(), diff.getNewAttribute(), DiffStatusEnum.DELETED))
                .forEach(rowDiff -> insertRowDiff(rowDiff, sheet, structureColumnIndexes));

        headRow.cellIterator().forEachRemaining(cell -> sheet.autoSizeColumn(cell.getColumnIndex(), true));
    }

    private Row createStructureHead(SXSSFSheet sheet) {

        structureColumnIndexes.put("code", 0);
        structureColumnIndexes.put("name", 1);
        structureColumnIndexes.put("type", 2);
        structureColumnIndexes.put("primary", 3);
        structureColumnIndexes.put("description", 5);

        Row row = createNextRow(sheet);
        row.createCell(0).setCellValue("Поле");
        row.createCell(1).setCellValue("Наименование");
        row.createCell(2).setCellValue("Тип данных");
        row.createCell(3).setCellValue("Первичный ключ");
        row.createCell(5).setCellValue("Описание");
        row.forEach(cell -> cell.setCellStyle(headStyle));
        return row;
    }

    private XlsxComparedRow createComparedRow(Structure.Attribute oldAttr, Structure.Attribute newAttr, DiffStatusEnum diffStatus) {

        oldAttr = oldAttr != null ? oldAttr : new Structure.Attribute();
        newAttr = newAttr != null ? newAttr : new Structure.Attribute();

        Map<String, XlsxComparedCell> diffs = new HashMap<>();
        diffs.put("code", new XlsxComparedCell(oldAttr.getCode(), newAttr.getCode(), diffStatus));
        diffs.put("name", new XlsxComparedCell(oldAttr.getName(), newAttr.getName(), diffStatus));
        diffs.put("type", new XlsxComparedCell(oldAttr.getType(), newAttr.getType(), diffStatus));

        diffs.put("primary", new XlsxComparedCell(oldAttr.hasIsPrimary(), newAttr.hasIsPrimary(), diffStatus));
        diffs.put("description", new XlsxComparedCell(oldAttr.getDescription(), newAttr.getDescription(), diffStatus));

        return new XlsxComparedRow(diffs, diffStatus);
    }

    private void addDataCompare() {

        final SXSSFSheet sheet = wb.createSheet("Данные справочника");
        RefBookDataDiff refBookDataDiff;
        try {
            refBookDataDiff = compareService.compareData(new CompareDataCriteria(oldVersion.getId(), newVersion.getId()));

        } catch (UserException e) {
            logger.info("cannot compare data", e);
            createNextRow(sheet).createCell(0).setCellValue("Невозможно сравнить данные");
            return;
        }
        createStatusCells(sheet);

        final Row headRow = createDataHead(
                sheet,
                refBookDataDiff.getAttributeDiff().getOldAttributes(),
                refBookDataDiff.getAttributeDiff().getNewAttributes()
        );
        sheet.trackAllColumnsForAutoSizing();

        final CompareDataCriteria compareCriteria = new CompareDataCriteria(
                oldVersion.getId(), newVersion.getId()
        );

        PageIterator<ComparableRow, CompareDataCriteria> pageIterator =
                new PageIterator<>(compareService::getCommonComparableRows, compareCriteria, true);
        pageIterator.forEachRemaining(page ->
                page.getContent().stream()
                        .map(comparableRow -> {
                            final Map<String, XlsxComparedCell> diffValueMap = new HashMap<>();
                            comparableRow.getFieldValues()
                                    .forEach(cfv -> diffValueMap.put(
                                            cfv.getComparableField().getCode(),
                                            new XlsxComparedCell(cfv.getOldValue(), cfv.getNewValue(), cfv.getStatus())));
                            return new XlsxComparedRow(diffValueMap, comparableRow.getStatus());
                        })
                        .forEach(rowDiffValue -> {
                            if (rowDiffValue.getCells().values().stream().anyMatch(cellDiffValue ->
                                    DiffStatusEnum.UPDATED.equals(cellDiffValue.getStatus())
                            )) {
                                rowDiffValue.setDiffStatus(DiffStatusEnum.UPDATED);
                            }

                            insertRowDiff(rowDiffValue, sheet, dataColumnIndexes);
                        })
        );

        headRow.cellIterator().forEachRemaining(cell ->
                sheet.autoSizeColumn(cell.getColumnIndex(), true)
        );
    }

    private Row createDataHead(SXSSFSheet sheet, List<String> deletedColumns, List<String> createdColumns) {

        final Row headRow = createNextRow(sheet);
        final Map<String, String> allAttributes = new HashMap<>();
        Stream.concat(
                oldVersion.getStructure().getAttributes().stream(),
                        newVersion.getStructure().getAttributes().stream()
                )
                .forEach(a -> allAttributes.put(a.getCode(), a.getName()));
        Stream.concat(
                newVersion.getStructure().getAttributeCodes().stream(),
                        deletedColumns.stream()
                )
                .forEach(attribute -> {
                    dataColumnIndexes.put(attribute, dataColumnIndexes.size());

                    final Cell cell = headRow.createCell(dataColumnIndexes.get(attribute));
                    cell.setCellValue(allAttributes.get(attribute));
                    if (deletedColumns.contains(attribute))
                        cell.setCellStyle(headDeleteStyle);
                    else if (createdColumns.contains(attribute))
                        cell.setCellStyle(headInsertStyle);
                    else
                        cell.setCellStyle(headStyle);
                });

        return headRow;
    }

    private void insertRowDiff(XlsxComparedRow rowDiff, SXSSFSheet sheet, Map<String, Integer> indexes) {

        Set<Integer> notInserted = new HashSet<>();
        notInserted.addAll(indexes.values());

        Row row = createNextRow(sheet);
        if (DiffStatusEnum.UPDATED.equals(rowDiff.getDiffStatus())) {
            getOrCreateRow(sheet, row.getRowNum() + 1);
        }

        rowDiff.getCells().forEach((key, value) -> {
            Integer column = indexes.get(key);
            if (DiffStatusEnum.UPDATED.equals(rowDiff.getDiffStatus()) &&
                    (value == null || !DiffStatusEnum.UPDATED.equals(value.getStatus()) ||
                            Objects.equals(value.getOldValue(), value.getNewValue()))) {
                sheet.addMergedRegion(new CellRangeAddress(
                        row.getRowNum(), row.getRowNum() + 1,
                        column, column
                ));
            }
            insertCellDiffValue(row.createCell(column), value);
            notInserted.remove(column);
        });
    }

    @SuppressWarnings("UnusedReturnValue")
    private Row getOrCreateRow(SXSSFSheet sheet, int rowIndex) {

        final Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private void createStatusCells(SXSSFSheet sheet) {

        sheet.trackColumnForAutoSizing(0);

        final Cell statusAddCell = sheet.createRow(0).createCell(0);
        statusAddCell.setCellValue("Добавлено");
        statusAddCell.setCellStyle(insertStyle);

        final Cell statusUpdCell = sheet.createRow(1).createCell(0);
        statusUpdCell.setCellValue("Изменено");
        statusUpdCell.setCellStyle(updNewStyle);

        final Cell statusDelCell = sheet.createRow(2).createCell(0);
        statusDelCell.setCellValue("Удалено");
        statusDelCell.setCellStyle(deleteStyle);

        sheet.createRow(3);
    }

    private void insertCellDiffValue(Cell cell, XlsxComparedCell xlsxComparedCell) {

        if (xlsxComparedCell == null) {
            cell.setCellStyle(deleteStyle);
            return;

        } else if (xlsxComparedCell.getStatus() == null) {
            cell.setCellStyle(defaultStyle);
            fillCell(cell, xlsxComparedCell.getNewValue());
            return;
        }

        switch (xlsxComparedCell.getStatus()) {
            case INSERTED -> {
                cell.setCellStyle(insertStyle);
                fillCell(cell, xlsxComparedCell.getNewValue());
            }
            case DELETED -> {
                cell.setCellStyle(deleteStyle);
                fillCell(cell, xlsxComparedCell.getOldValue());
            }
            case UPDATED -> {
                cell.setCellStyle(updNewStyle);
                fillCell(cell, xlsxComparedCell.getNewValue());
                Cell oldCell = cell.getSheet().getRow(cell.getRowIndex() + 1).createCell(cell.getColumnIndex());
                oldCell.setCellStyle(updOldStyle);
                fillCell(oldCell, xlsxComparedCell.getOldValue());
            }
        }
    }

    private void fillCell(Cell cell, Object value) {

        if (value instanceof LocalDate) {
            Date date = Date.from(((LocalDate) value).atStartOfDay(ZoneId.systemDefault()).toInstant());
            cell.setCellStyle(getDateCellStyle(cell.getCellStyle()));
            cell.setCellValue(date);

        } else if (value instanceof Boolean) {
            cell.setCellStyle(cell.getCellStyle());
            cell.setCellValue((Boolean) value);

        } else if (value instanceof Number) {
            cell.setCellStyle(cell.getCellStyle());
            cell.setCellValue(((Number) value).doubleValue());

        } else if (value instanceof Reference) {
            cell.setCellStyle(cell.getCellStyle());
            cell.setCellValue(((Reference) value).getValue());

        } else {
            cell.setCellStyle(cell.getCellStyle());
            cell.setCellValue(Optional.ofNullable(value).orElse("").toString());
        }
    }

    private CellStyle getDateCellStyle(CellStyle style) {

        if (insertStyle.equals(style))
            return insertDateStyle;
        else if (updNewStyle.equals(style))
            return updNewDateStyle;
        else if (updOldStyle.equals(style))
            return updOldDateStyle;
        else if (deleteStyle.equals(style))
            return deleteDateStyle;
        else
            return dateStyle;
    }

    public static class XlsxComparedRow {

        private final Map<String, XlsxComparedCell> cells;
        private DiffStatusEnum diffStatus;

        public XlsxComparedRow(Map<String, XlsxComparedCell> cells, DiffStatusEnum diffStatus) {
            this.cells = cells;
            this.diffStatus = diffStatus;
        }

        public Map<String, XlsxComparedCell> getCells() {
            return cells;
        }

        public DiffStatusEnum getDiffStatus() {
            return diffStatus;
        }

        public void setDiffStatus(DiffStatusEnum diffStatus) {
            this.diffStatus = diffStatus;
        }
    }

    public static class XlsxComparedCell {

        private final Object oldValue;
        private final Object newValue;
        private DiffStatusEnum status;

        public XlsxComparedCell(Object oldValue, Object newValue, DiffStatusEnum status) {

            this.oldValue = oldValue;
            this.newValue = newValue;
            this.status = DiffStatusEnum.UPDATED.equals(status) &&
                    Objects.equals(oldValue, newValue) ? null : status;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }

        public DiffStatusEnum getStatus() {
            return status;
        }

        public void setStatus(DiffStatusEnum status) {
            this.status = status;
        }
    }

}
