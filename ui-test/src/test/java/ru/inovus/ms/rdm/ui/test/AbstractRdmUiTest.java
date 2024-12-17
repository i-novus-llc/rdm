package ru.inovus.ms.rdm.ui.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.selenide.AllureSelenide;
import lombok.extern.slf4j.Slf4j;
import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.component.DropDown;
import net.n2oapp.framework.autotest.api.component.control.Control;
import net.n2oapp.framework.autotest.api.component.control.InputText;
import net.n2oapp.framework.autotest.api.component.control.TextArea;
import net.n2oapp.framework.autotest.impl.component.control.N2oCheckbox;
import net.n2oapp.framework.autotest.impl.component.control.N2oDateInput;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputSelect;
import net.n2oapp.framework.autotest.impl.component.control.N2oSelect;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.system.SystemProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.inovus.ms.rdm.ui.test.model.FieldType;
import ru.inovus.ms.rdm.ui.test.model.RefBook;
import ru.inovus.ms.rdm.ui.test.model.RefBookField;
import ru.inovus.ms.rdm.ui.test.page.AttributeForm;
import ru.inovus.ms.rdm.ui.test.page.CreateRefBookWidget;
import ru.inovus.ms.rdm.ui.test.page.DataGridWidget;
import ru.inovus.ms.rdm.ui.test.page.DataRowForm;
import ru.inovus.ms.rdm.ui.test.page.DataWithConflictsWidget;
import ru.inovus.ms.rdm.ui.test.page.RdmLogin;
import ru.inovus.ms.rdm.ui.test.page.RefBookEditPage;
import ru.inovus.ms.rdm.ui.test.page.RefBookListPage;
import ru.inovus.ms.rdm.ui.test.page.StructureWidget;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@Slf4j
@ExtendWith(SpringExtension.class)
abstract class AbstractRdmUiTest {

    private static final String USERNAME = SystemProperties.get("rdm.username");
    private static final String PASSWORD = SystemProperties.get("rdm.password");

    // Время создания справочника (для локализации ошибки).
    private static final ZoneId UNIVERSAL_TIMEZONE = ZoneId.of("UTC");
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private static final int DATA_ROWS_CREATE_COUNT = 3;
    private static final long SLEEP_TIME = TimeUnit.SECONDS.toMillis(6);

    // Все простые поля для проверки справочников.
    private static final List<FieldType> DEFAULT_FIELD_TYPES = List.of(
            // В списке дата всегда должна быть последней, иначе календарь перекрывает другие поля.
            FieldType.INTEGER, FieldType.STRING, FieldType.DOUBLE, FieldType.BOOLEAN, FieldType.DATE
    );

    // Минимальное количество полей для проверки ссылочных справочников.
    private static final List<FieldType> REFERRED_FIELD_TYPES = List.of(
            FieldType.INTEGER, FieldType.STRING
    );
    private static final List<FieldType> REFERRER_FIELD_TYPES = List.of(
            // STRING нужен, иначе вылетает по таймауту.
            FieldType.INTEGER, FieldType.STRING, FieldType.REFERENCE
    );

    // Наименования атрибутов/полей.
    private static final String ATTR_ID_NAME = "Идентификатор";
    private static final String ATTR_NAME_NAME = "Наименование";
    private static final String ATTR_REFERENCE_NAME = "Ссылочное поле";

    // Формат поля даты при редактировании значения.
    private static final String EDIT_FIELD_DATE_PATTERN = "dd.MM.yyyy";
    private static final DateTimeFormatter EDIT_FIELD_DATE_FORMATTER = DateTimeFormatter.ofPattern(EDIT_FIELD_DATE_PATTERN);

    // Легче работать с упорядоченным по id списком, поэтому через этот класс задаём порядок.
    private final AtomicInteger refBookDataIdSeq = new AtomicInteger();

    @BeforeAll
    static void setUpAll() {
        SelenideLogger.addListener("AllureSelenide", new AllureSelenide());
    }

    @BeforeEach
    void setUp() {
        RdmLogin.login(USERNAME, PASSWORD);
    }

    @AfterAll
    static void tearDown() {
        Selenide.closeWebDriver();
    }

    void testRefBook(String type) {

        final RefBook refBook = generateRefBook(type, DATA_ROWS_CREATE_COUNT, DEFAULT_FIELD_TYPES, null);

        final RefBookListPage refBookListPage = N2oSelenide.page(RefBookListPage.class);
        refBookListPage.shouldExists();

        // Создание.
        createRefBook(refBookListPage, refBook);
        refBookListPage.shouldExists();

        search(refBookListPage, refBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(refBook.getCode()));

        // Редактирование.
        final RefBookEditPage refBookEditPage = openRefBookEditPage(refBookListPage, 0);
        editRefBook(refBookEditPage, refBook);
        search(refBookListPage, refBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(refBook.getCode()));

        // Удаление.
        refBookListPage.deleteRow(0);
        search(refBookListPage, refBook);
        refBookListPage.rowShouldHaveSize(0);
    }

    void testReferrerRefBook(String referredType, String referrerType) {

        final RefBook referredBook = generateRefBook(referredType, DATA_ROWS_CREATE_COUNT, REFERRED_FIELD_TYPES, null);
        final RefBook referrerBook = generateRefBook(referrerType, DATA_ROWS_CREATE_COUNT, REFERRER_FIELD_TYPES, referredBook);

        final RefBookListPage refBookListPage = N2oSelenide.page(RefBookListPage.class);
        refBookListPage.shouldExists();

        // Создание обычного справочника.
        createRefBook(refBookListPage, referredBook);
        refBookListPage.shouldExists();

        search(refBookListPage, referredBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(referredBook.getCode()));
//        if (Objects.equals(referredType, RefBook.getUnversionedType())) {
//            publishRefBook(refBookListPage.openRefBookEditPage(0));
//        }

        // Создание ссылочного справочника.
        createRefBook(refBookListPage, referrerBook);

        // Создание конфликтов.
        search(refBookListPage, referredBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(referredBook.getCode()));

        RefBookEditPage refBookEditPage = openRefBookEditPage(refBookListPage, 0);
        createDataConflicts(refBookEditPage, referredBook);

        // Разрешение конфликтов.
        search(refBookListPage, referrerBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(referrerBook.getCode()));

        refBookEditPage = openRefBookEditPage(refBookListPage, 0);
        resolveDataConflicts(refBookEditPage, referrerBook);

        // Удаление ссылочного справочника.
        search(refBookListPage, referrerBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(referrerBook.getCode()));

        refBookListPage.deleteRow(0);
        search(refBookListPage, referrerBook);
        refBookListPage.rowShouldHaveSize(0);

        // Удаление обычного справочника.
        search(refBookListPage, referredBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(referredBook.getCode()));

        refBookListPage.deleteRow(0);
        search(refBookListPage, referredBook);
        refBookListPage.rowShouldHaveSize(0);
    }

    private void search(RefBookListPage refBookListPage, RefBook refBook) {

        fillInputControl(refBookListPage.codeFilter(), refBook.getCode());
        fillInputControl(refBookListPage.nameFilter(), refBook.getName());
        refBookListPage.shouldExists();

        refBookListPage.search();
    }

    private void createRefBook(RefBookListPage refBookListPage, RefBook refBook) {

        final CreateRefBookWidget createRefBookWidget = refBookListPage.openCreateRefBookPage();
        createRefBookWidget.shouldExists();

        fillRefBookWidget(createRefBookWidget, refBook);
        final RefBookEditPage refBookEditPage = createRefBookWidget.save();
        refBookEditPage.shouldExists();

        final StructureWidget structureWidget = refBookEditPage.structure();
        structureWidget.shouldExists();

        final Set<RefBookField> fieldsToFirstRefBook = refBook.getRows().get(0).keySet();
        createStructure(structureWidget, fieldsToFirstRefBook);

        final DataGridWidget dataGridWidget = refBookEditPage.data();
        dataGridWidget.shouldExists();

        final List<String> addedRowsNameColumnValues = new ArrayList<>();
        for (Map<RefBookField, Object> row : refBook.getRows()) {

            final DataRowForm dataForm = dataGridWidget.openAddRowForm();
            dataForm.shouldExists();

            fillDataRowForm(dataForm, row);
            dataForm.save();

            addedRowsNameColumnValues.add(getNameColumnValue(row));
            dataGridWidget.rowShouldHaveTexts(1, addedRowsNameColumnValues);
        }

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void createStructure(StructureWidget structureWidget, Set<RefBookField> fieldsToFirstRefBook) {

        final List<String> addedRowsCodeColumnValues = new ArrayList<>(fieldsToFirstRefBook.size());
        final List<String> addedRowsNameColumnValues = new ArrayList<>(fieldsToFirstRefBook.size());

        fieldsToFirstRefBook.forEach(field -> {

            final AttributeForm attributeForm = structureWidget.openAddForm();
            attributeForm.shouldExists();

            fillAttributeForm(attributeForm, field);
            attributeForm.save();

            addedRowsCodeColumnValues.add(field.getCode());
            addedRowsNameColumnValues.add(field.getName());

            structureWidget.rowShouldHaveTexts(1, addedRowsCodeColumnValues);
            structureWidget.rowShouldHaveTexts(2, addedRowsNameColumnValues);
        });
    }

    private void editRefBook(RefBookEditPage refBookEditPage, RefBook refBook) {

        List<Map<RefBookField, Object>> existedRows = refBook.getRows();
        List<String> nameColumnValues = existedRows.stream().map(this::getNameColumnValue).collect(toList());

        refBookEditPage.shouldExists();
        DataGridWidget dataGridWidget = refBookEditPage.data();

        final List<Map<RefBookField, Object>> refBookRows = generateRows(1, DEFAULT_FIELD_TYPES, null);
        final Map<RefBookField, Object> row = refBookRows.get(0);

        final DataRowForm addForm = dataGridWidget.openAddRowForm();
        fillDataRowForm(addForm, row);
        addForm.save();

        nameColumnValues.add(getNameColumnValue(row));
        dataGridWidget.rowShouldHaveTexts(1, nameColumnValues);

        final int lastRowNum = nameColumnValues.size() - 1;
        final DataRowForm editForm = dataGridWidget.openEditRowForm(lastRowNum);
        final String newNameValue = "Другое наименование";
        fillInputControl(editForm.stringInput(ATTR_NAME_NAME), newNameValue);
        editForm.edit();

        nameColumnValues.set(lastRowNum, newNameValue);
        dataGridWidget.rowShouldHaveTexts(1, nameColumnValues);

        dataGridWidget.deleteRow(lastRowNum);

        nameColumnValues.remove(lastRowNum);
        dataGridWidget.rowShouldHaveTexts(1, nameColumnValues);

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void createDataConflicts(RefBookEditPage refBookEditPage, RefBook refBook) {

        final List<Map<RefBookField, Object>> existedRows = refBook.getRows();
        final List<String> nameColumnValues = existedRows.stream().map(this::getNameColumnValue).collect(toList());

        refBookEditPage.shouldExists();
        DataGridWidget dataGridWidget = refBookEditPage.data();

        // Конфликт DELETED.
        dataGridWidget.deleteRow(0);

        nameColumnValues.remove(0);
        dataGridWidget.rowShouldHaveTexts(1, nameColumnValues);

        // Конфликт UPDATED.
        final DataRowForm editForm = dataGridWidget.openEditRowForm(0);
        final String newNameValue = nameColumnValues.get(0) + "_updated";
        fillInputControl(editForm.stringInput(ATTR_NAME_NAME), newNameValue);
        editForm.edit();

        nameColumnValues.set(0, newNameValue);
        dataGridWidget.rowShouldHaveTexts(1, nameColumnValues);

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void resolveDataConflicts(RefBookEditPage refBookEditPage, RefBook referrer) {

        final List<Map<RefBookField, Object>> existedRows = referrer.getRows();
        final List<String> nameColumnValues = existedRows.stream().map(this::getNameColumnValue).collect(toList());
        final List<String> nameColumnConflictedValues = nameColumnValues.subList(0, 2);

        refBookEditPage.shouldExists();
        //final DataWidget dataWidget = refBookEditPage.data();
        //dataWidget.rowShouldHaveTexts(1, nameColumnValues);

        DataWithConflictsWidget dataWithConflictsWidget = refBookEditPage.dataWithConflicts();
        dataWithConflictsWidget.rowShouldHaveSize(2); // Два конфликта
        dataWithConflictsWidget.rowShouldHaveTexts(1, nameColumnConflictedValues);

        refBookEditPage.refreshReferrer();
        waitActionResult(SLEEP_TIME);

        nameColumnConflictedValues.remove(1);
        dataWithConflictsWidget = refBookEditPage.dataWithConflicts();
        dataWithConflictsWidget.rowShouldHaveSize(1); // Один конфликт
        dataWithConflictsWidget.rowShouldHaveTexts(1, nameColumnConflictedValues);

        final DataRowForm editForm = dataWithConflictsWidget.fixRowForm(0);
        fillReference(editForm.referenceInput(ATTR_REFERENCE_NAME), 0);
        editForm.edit();

        dataWithConflictsWidget = refBookEditPage.dataWithConflicts();
        Selenide.refresh();
        dataWithConflictsWidget.rowShouldHaveSize(0); // Нет конфликтов

        openRefBookListPage();
    }

    private void openRefBookListPage() {

        N2oSelenide.open("/", RefBookListPage.class);
    }

    private RefBookEditPage openRefBookEditPage(RefBookListPage refBookListPage, int rowNum) {

        return refBookListPage.openRefBookEditPage(rowNum);
    }

    private void publishRefBook(RefBookEditPage refBookEditPage) {

        refBookEditPage.publish();
        waitActionResult(SLEEP_TIME);
    }

    private void fillDataRowForm(DataRowForm form, Map<RefBookField, Object> row) {

        row.forEach((key, value) -> {
            switch (key.getType()) {
                case STRING: fillInputControl(form.stringInput(key.getName()), value.toString()); break;
                case INTEGER: fillInputControl(form.integerInput(key.getName()), value.toString()); break;
                case DOUBLE: fillInputControl(form.doubleInput(key.getName()), value.toString()); break;
                case DATE: fillInputControl(form.dateInput(key.getName()), (value.toString())); break;
                case BOOLEAN: fillCheckBox(form.booleanInput(key.getName()), true); break;
                case REFERENCE: fillReference(form.referenceInput(key.getName()), value); break;
                default: throw new IllegalArgumentException(key.getType() + " invalid field type");
            }
        });

    }

    private void fillAttributeForm(AttributeForm form, RefBookField refBookField) {

        fillInputControl(form.codeInput(), refBookField.getCode());
        fillInputControl(form.nameInput(), refBookField.getName());

        final N2oSelect typeInput = form.typeInput();
        typeInput.openPopup();
        final DropDown typeDropDown = typeInput.dropdown();
        typeDropDown.selectItemBy(Condition.text(refBookField.getType().getLabel()));
        typeInput.shouldHaveValue(refBookField.getType().getLabel());

        if (refBookField.isReferenceType()) {

            final N2oInputSelect refBookInput = form.refBookInput();
            refBookInput.setValue(refBookField.getReferredBook().getCode());
            final DropDown refBookDropDown = refBookInput.dropdown();
            refBookDropDown.shouldHaveOptions(1);
            refBookDropDown.selectItem(0);

            final N2oInputSelect displayAttrInput = form.displayAttrInput();
            final Map.Entry<Integer, String> referredField = refBookField.getReferredField();
            displayAttrInput.setValue(referredField.getValue());
            final DropDown displayAttrInputDropDown = displayAttrInput.dropdown();
            displayAttrInputDropDown.shouldHaveOptions(1);
            displayAttrInputDropDown.selectItem(0);

        }

        if (refBookField.isPrimary()) {

            fillCheckBox(form.primaryKeyInput(), true);
        }
    }

    private void fillRefBookWidget(CreateRefBookWidget widget, RefBook refBook) {

        fillInputControl(widget.codeInput(), refBook.getCode());
        fillInputControl(widget.nameInput(), refBook.getName());
        fillInputControl(widget.shortNameInput(), refBook.getShortName());
        fillInputControl(widget.descriptionInput(), refBook.getDescription());

        if (refBook.getType() != null) {

            final N2oInputSelect typeInput = widget.typeInput();
            typeInput.openPopup();
            final DropDown dropdown = typeInput.dropdown();
            dropdown.selectItemBy(Condition.text(refBook.getType()));
            typeInput.closePopup();
            typeInput.shouldHaveValue(refBook.getType());

        }
    }

    private void fillInputControl(Control control, String value) {

        control.shouldExists();

        if (control instanceof InputText) {
            ((InputText) control).setValue(value);

        } else if(control instanceof TextArea) {
            ((TextArea) control).setValue(value);

        } else if(control instanceof N2oDateInput) {
            ((N2oDateInput) control).setValue(value);

        } else {
            throw new IllegalArgumentException("Control is not for input");
        }

        control.shouldHaveValue(value);
    }

    @SuppressWarnings("SameParameterValue")
    private void fillCheckBox(Control checkBox, boolean value) {

        checkBox.shouldExists();

        if (!(checkBox instanceof N2oCheckbox))
            throw new IllegalArgumentException("Control is not check box");

        final N2oCheckbox control = (N2oCheckbox) checkBox;
        control.setChecked(value);

        if (value) {
            control.shouldBeChecked();
        } else {
            control.shouldBeEmpty();
        }
    }

    private void fillReference(Control referenceInput, Object value) {

        referenceInput.shouldExists();

        if (!(referenceInput instanceof N2oInputSelect))
            throw new IllegalArgumentException("Control is not reference input");

        final N2oInputSelect control = (N2oInputSelect) referenceInput;
        // to-do: Очищать только при наличии значения!
        control.clear();
        waitActionResult(SLEEP_TIME / 2);
        
        final DropDown dropDown = control.dropdown();
        dropDown.selectItem((int) value);
        //control.select((int) value);
    }

    /** Ожидание результата действия. */
    private void waitActionResult(long sleepTime) {
        
        Selenide.sleep(sleepTime != 0 ? sleepTime : 1);
    }

    /** Формирование информации о справочнике для создания. */
    @SuppressWarnings("SameParameterValue")
    private RefBook generateRefBook(String type, int rowCount, List<FieldType> fieldTypes, RefBook referredBook) {

        return new RefBook(
                "a_" + RandomStringUtils.randomAlphabetic(5),
                "a " + RandomStringUtils.randomAlphabetic(5) +
                        (referredBook != null ? " to " + referredBook.getCode() : "") +
                        " (" + now().format(DATE_TIME_FORMATTER) + ")",
                "shortName",
                "system",
                "description",
                type,
                generateRows(rowCount, fieldTypes, referredBook)
        );
    }

    /** Формирование информации о записях справочника. */
    private List<Map<RefBookField, Object>> generateRows(int rowCount,
                                                         List<FieldType> fieldTypes,
                                                         RefBook referredBook) {

        final List<Map<RefBookField, Object>> result = new ArrayList<>(rowCount);
        for(int i = 0; i < rowCount; i++) {
            Map<RefBookField, Object> row = new LinkedHashMap<>(fieldTypes.size());
            for (FieldType fieldType : fieldTypes) {
                switch (fieldType) {
                    case STRING: row.put(
                            new RefBookField("name", ATTR_NAME_NAME, fieldType, false),
                            RandomStringUtils.randomAlphabetic(5)
                    ); break;
                    case INTEGER: row.put(
                            new RefBookField("id", ATTR_ID_NAME, fieldType, true),
                            String.valueOf(refBookDataIdSeq.getAndIncrement())
                    ); break;
                    case DOUBLE: row.put(
                            new RefBookField("some_double", "Некоторое дробное поле", fieldType, false),
                            String.valueOf(RandomUtils.nextDouble(1.01, 2)).substring(0, 3)
                    ); break;
                    case DATE: row.put(
                            new RefBookField("some_date", "Некоторая дата", fieldType, false),
                            LocalDate.now().format(EDIT_FIELD_DATE_FORMATTER)
                    ); break;
                    case BOOLEAN: row.put(
                            new RefBookField("some_boolean", "Некоторое логическое поле", fieldType,
                                    false), RandomUtils.nextBoolean()
                    ); break;
                    case REFERENCE: row.put(
                            new RefBookField("some_reference", ATTR_REFERENCE_NAME, fieldType, false,
                                    referredBook, new AbstractMap.SimpleEntry<>(1, ATTR_NAME_NAME)),
                            referredBook != null && referredBook.getRows().size() > i ? i : null
                    ); break;
                    default: throw new IllegalArgumentException(fieldType + " invalid field type");
                }
            }
            result.add(row);
        }
        return result;
    }

    private String getNameColumnValue(Map<RefBookField, Object> row) {

        Map.Entry<RefBookField, Object> field = row.entrySet().stream()
                        .filter(entry -> "name".equals(entry.getKey().getCode()))
                        .findAny().orElse(null);

        return (field != null) ? (String) field.getValue() : null;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(UNIVERSAL_TIMEZONE);
    }
}
