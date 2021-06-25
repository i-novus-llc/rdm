package ru.inovus.ms.rdm.ui.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import net.n2oapp.framework.autotest.api.component.control.Control;
import net.n2oapp.framework.autotest.api.component.control.InputText;
import net.n2oapp.framework.autotest.api.component.control.TextArea;
import net.n2oapp.framework.autotest.impl.component.control.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.SystemProperties;
import ru.inovus.ms.rdm.ui.test.model.FieldType;
import ru.inovus.ms.rdm.ui.test.model.RefBook;
import ru.inovus.ms.rdm.ui.test.model.RefBookField;
import ru.inovus.ms.rdm.ui.test.page.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static net.n2oapp.framework.autotest.N2oSelenide.open;

public class RdmUiTest {

    private static final Logger logger = LoggerFactory.getLogger(RdmUiTest.class);

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

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
            // n2o 7.16: STRING нужен, иначе вылетает по таймауту.
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

    @BeforeClass
    public static void setUp() {

        Configuration.baseUrl = getBaseUrl();
        Configuration.timeout = 60000; // n2o 7.16: увеличен с 8000, иначе вылетает по таймауту.
    }

    // URL запущенного фронтенда RDM.
    private static String getBaseUrl() {

        String baseUrl = SystemProperties.get("rdm.url");
        if (baseUrl == null) {
            baseUrl = "http://localhost:8080";
        }
        return baseUrl;
    }

    @AfterClass
    public static void tearDown() {
        Selenide.closeWebDriver();
    }

    @After
    public void logout() {
        open("/logout", LoginPage.class).shouldExists();
    }

    /**
     * Проверка работы с обычным (версионным) справочником.
     */
    @Test
    public void testCreateDefaultRefBook() {
        testRefBook(null);
    }

    /**
     * Проверка работы с неверсионным справочником.
     */
    @Test
    public void testUnversionedRefBook() {
        testRefBook(RefBook.getUnversionedType());
    }

    private void testRefBook(String type) {

        RefBook refBook = generateRefBook(type, DATA_ROWS_CREATE_COUNT, DEFAULT_FIELD_TYPES, null);

        RefBookListPage refBookListPage = login();
        refBookListPage.shouldExists();

        // Создание.
        createRefBook(refBookListPage, refBook);
        refBookListPage.shouldExists();

        search(refBookListPage, refBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(refBook.getCode()));

        // Редактирование.
        RefBookEditPage refBookEditPage = openRefBookEditPage(refBookListPage, 0);
        editRefBook(refBookEditPage, refBook);
        search(refBookListPage, refBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(refBook.getCode()));

        // Удаление.
        refBookListPage.deleteRow(0);
        search(refBookListPage, refBook);
        refBookListPage.rowShouldHaveSize(0);
    }

    /**
     * Проверка работы с обычным справочником, ссылающимся на обычный справочник.
     */
    @Test
    public void testDefaultReferrerToDefault() {
        testReferrerRefBook(null, null);
    }

    /**
     * Проверка работы с обычным справочником, ссылающимся на неверсионный справочник.
     */
    @Test
    public void testDefaultReferrerToUnversioned() {
        testReferrerRefBook(RefBook.getUnversionedType(), null);
    }

    private void testReferrerRefBook(String referredType, String referrerType) {

        RefBook referredBook = generateRefBook(referredType, DATA_ROWS_CREATE_COUNT, REFERRED_FIELD_TYPES, null);
        RefBook referrerBook = generateRefBook(referrerType, DATA_ROWS_CREATE_COUNT, REFERRER_FIELD_TYPES, referredBook);

        RefBookListPage refBookListPage = login();
        refBookListPage.shouldExists();

        // Создание обычного справочника.
        createRefBook(refBookListPage, referredBook);
        refBookListPage.shouldExists();

        search(refBookListPage, referredBook);
        refBookListPage.rowShouldHaveTexts(0, singletonList(referredBook.getCode()));

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

        fillInputText(refBookListPage.codeFilter(), refBook.getCode());
        fillInputText(refBookListPage.nameFilter(), refBook.getName());
        refBookListPage.shouldExists();

        refBookListPage.search();
    }

    private void createRefBook(RefBookListPage refBookListPage, RefBook refBook) {

        RefBookCreateFormWidget refBookCreateFormWidget = refBookListPage.openCreateFormPage();
        refBookCreateFormWidget.shouldExists();

        fillRefBookForm(refBookCreateFormWidget, refBook);
        RefBookEditPage refBookEditPage = refBookCreateFormWidget.save();
        refBookEditPage.shouldExists();

        StructureListWidget structureListWidget = refBookEditPage.structure();
        structureListWidget.shouldExists();

        Set<RefBookField> fieldsToFirstRefBook = refBook.getRows().get(0).keySet();
        createStructure(structureListWidget, fieldsToFirstRefBook);

        DataListWidget dataListWidget = refBookEditPage.data();
        dataListWidget.shouldExists();

        List<String> addedRowsNameColumnValues = new ArrayList<>();
        for (Map<RefBookField, Object> row : refBook.getRows()) {

            DataFormModal dataForm = dataListWidget.addRowForm();
            dataForm.shouldExists();

            fillDataForm(dataForm, row);
            dataForm.save();

            addedRowsNameColumnValues.add(getNameColumnValue(row));
            dataListWidget.rowShouldHaveTexts(1, addedRowsNameColumnValues);
        }

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void createStructure(StructureListWidget structureListWidget, Set<RefBookField> fieldsToFirstRefBook) {

        List<String> addedRowsCodeColumnValues = new ArrayList<>();
        List<String> addedRowsNameColumnValues = new ArrayList<>();

        fieldsToFirstRefBook.forEach(field -> {

            StructureFormModal structureForm = structureListWidget.form();
            structureForm.shouldExists();

            fillStructureForm(structureForm, field);
            structureForm.save();

            addedRowsCodeColumnValues.add(field.getCode());
            addedRowsNameColumnValues.add(field.getName());
            structureListWidget.rowShouldHaveTexts(1, addedRowsCodeColumnValues);
            structureListWidget.rowShouldHaveTexts(2, addedRowsNameColumnValues);
        });
    }

    private void editRefBook(RefBookEditPage refBookEditPage, RefBook refBook) {

        List<Map<RefBookField, Object>> existedRows = refBook.getRows();
        List<String> nameColumnValues = existedRows.stream()
                .map(this::getNameColumnValue)
                .collect(Collectors.toList());

        refBookEditPage.shouldExists();
        DataListWidget dataListWidget = refBookEditPage.data();

        List<Map<RefBookField, Object>> refBookRows = generateRefBookRows(1, DEFAULT_FIELD_TYPES, null);
        Map<RefBookField, Object> row = refBookRows.get(0);

        DataFormModal addForm = dataListWidget.addRowForm();
        fillDataForm(addForm, row);
        addForm.save();

        nameColumnValues.add(getNameColumnValue(row));
        dataListWidget.rowShouldHaveTexts(1, nameColumnValues);

        final int lastRowNum = nameColumnValues.size() - 1;
        DataFormModal editForm = dataListWidget.editRowForm(lastRowNum);
        String newNameValue = "Другое наименование";
        fillInputText(editForm.stringInput(ATTR_NAME_NAME), newNameValue);
        editForm.edit();

        nameColumnValues.set(lastRowNum, newNameValue);
        dataListWidget.rowShouldHaveTexts(1, nameColumnValues);

        dataListWidget.deleteRowForm(lastRowNum);

        nameColumnValues.remove(lastRowNum);
        dataListWidget.rowShouldHaveTexts(1, nameColumnValues);

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void createDataConflicts(RefBookEditPage refBookEditPage, RefBook refBook) {

        List<Map<RefBookField, Object>> existedRows = refBook.getRows();
        List<String> nameColumnValues = existedRows.stream()
                .map(this::getNameColumnValue)
                .collect(Collectors.toList());

        refBookEditPage.shouldExists();
        DataListWidget dataListWidget = refBookEditPage.data();

        // Конфликт DELETED.
        dataListWidget.deleteRowForm(0);

        nameColumnValues.remove(0);
        dataListWidget.rowShouldHaveTexts(1, nameColumnValues);

        // Конфликт UPDATED.
        DataFormModal editForm = dataListWidget.editRowForm(0);
        String newNameValue = nameColumnValues.get(0) + "_updated";
        fillInputText(editForm.stringInput(ATTR_NAME_NAME), newNameValue);
        editForm.edit();

        nameColumnValues.set(0, newNameValue);
        dataListWidget.rowShouldHaveTexts(1, nameColumnValues);

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void resolveDataConflicts(RefBookEditPage refBookEditPage, RefBook referrer) {

        List<Map<RefBookField, Object>> existedRows = referrer.getRows();
        List<String> nameColumnValues = existedRows.stream()
                .map(this::getNameColumnValue)
                .collect(Collectors.toList());
        List<String> nameColumnConflictedValues = nameColumnValues.subList(0, 2);

        refBookEditPage.shouldExists();
        //DataListWidget dataListWidget = refBookEditPage.data();
        //dataListWidget.rowShouldHaveTexts(1, nameColumnValues);

        DataWithConflictsListWidget dataWithConflictsListWidget = refBookEditPage.dataWithConflicts();
        dataWithConflictsListWidget.rowShouldHaveSize(2); // Два конфликта
        dataWithConflictsListWidget.rowShouldHaveTexts(1, nameColumnConflictedValues);

        refBookEditPage.refreshReferrer();
        waitActionResult();

        nameColumnConflictedValues.remove(1);
        dataWithConflictsListWidget = refBookEditPage.dataWithConflicts();
        dataWithConflictsListWidget.rowShouldHaveSize(1); // Один конфликт
        dataWithConflictsListWidget.rowShouldHaveTexts(1, nameColumnConflictedValues);

        DataFormModal editForm = dataWithConflictsListWidget.fixRowForm(0);
        fillReference(editForm.referenceInput(ATTR_REFERENCE_NAME), 0);
        editForm.edit();

        dataWithConflictsListWidget = refBookEditPage.dataWithConflicts();
        dataWithConflictsListWidget.rowShouldHaveSize(0); // Нет конфликтов

        openRefBookListPage();
    }

    private void openRefBookListPage() {

        open("/", RefBookListPage.class);
    }

    private RefBookEditPage openRefBookEditPage(RefBookListPage refBookListPage, int rowNum) {

        return refBookListPage.openRefBookEditPage(rowNum);
    }

    private void publishRefBook(RefBookEditPage refBookEditPage) {

        refBookEditPage.publish();
        waitActionResult();
    }

    private void fillDataForm(DataFormModal dataForm, Map<RefBookField, Object> row) {

        row.forEach((key, value) -> {
            switch (key.getType()) {
                case STRING -> fillInputText(dataForm.stringInput(key.getName()), value.toString());
                case INTEGER -> fillInputText(dataForm.integerInput(key.getName()), value.toString());
                case DOUBLE -> fillInputText(dataForm.doubleInput(key.getName()), value.toString());
                case DATE -> fillInputText(dataForm.dateInput(key.getName()), (value.toString()));
                case BOOLEAN -> fillCheckBox(dataForm.booleanInput(key.getName()), true);
                case REFERENCE -> fillReference(dataForm.referenceInput(key.getName()), value);
                default -> throw new IllegalArgumentException(key.getType() + " invalid field type");
            }
        });

    }

    private void fillStructureForm(StructureFormModal structureForm, RefBookField refBookField) {

        fillInputText(structureForm.codeInput(), refBookField.getCode());
        fillInputText(structureForm.nameInput(), refBookField.getName());

        N2oSelect typeInput = structureForm.typeInput();
        typeInput.select(Condition.text(refBookField.getType().getTranslated()));
        typeInput.shouldHaveValue(refBookField.getType().getTranslated());

        if (refBookField.isReferenceType()) {
            N2oInputSelect refBookInput = structureForm.refBookInput();
            refBookInput.shouldBeEmpty();
            refBookInput.val(refBookField.getReferredBook().getCode());
            refBookInput.expandPopUpOptions();
            refBookInput.select(Condition.text(refBookField.getReferredBook().getCode()));
            refBookInput.shouldSelected(refBookField.getReferredBook().getCode());

            N2oInputSelect displayAttrInput = structureForm.displayAttrInput();
            Map.Entry<Integer, String> referredField = refBookField.getReferredField();
            if (referredField != null) {
                displayAttrInput.select(referredField.getKey());
                displayAttrInput.shouldHaveValue(referredField.getValue());
            }
        }

        if (refBookField.isPrimary()) {
            fillCheckBox(structureForm.primaryKeyInput(), true);
        }
    }

    private void fillRefBookForm(RefBookCreateFormWidget refBookCreateFormWidget, RefBook refBook) {

        fillInputText(refBookCreateFormWidget.codeInput(), refBook.getCode());
        fillInputText(refBookCreateFormWidget.nameInput(), refBook.getName());
        fillInputText(refBookCreateFormWidget.shortNameInput(), refBook.getShortName());
        fillInputText(refBookCreateFormWidget.descriptionInput(), refBook.getDescription());

        if (refBook.getType() != null) {

            N2oInputSelect typeInput = refBookCreateFormWidget.typeInput();
            typeInput.shouldBeEmpty();
            typeInput.val(refBook.getType());
            typeInput.expandPopUpOptions();
            typeInput.select(Condition.text(refBook.getType()));
            typeInput.shouldSelected(refBook.getType());
        }
    }

    private void fillInputText(Control inputText, String value) {

        inputText.shouldExists();

        if (inputText instanceof InputText) {
            ((InputText) inputText).val(value);

        } else if(inputText instanceof TextArea) {
            ((TextArea) inputText).val(value);

        } else if(inputText instanceof N2oDateInput) {
            ((N2oDateInput) inputText).val(value);

        } else {
            throw new IllegalArgumentException("control is not text input");
        }

        inputText.shouldHaveValue(value);
    }

    private void fillCheckBox(Control checkBox, boolean value) {

        checkBox.shouldExists();

        if (!(checkBox instanceof N2oCheckbox))
            throw new IllegalArgumentException("control is not check box");

        N2oCheckbox control = (N2oCheckbox) checkBox;

        if (value) {
            control.setChecked(value);
            control.shouldBeChecked();

        } else {
            control.setChecked(value);
            control.shouldBeEmpty();
        }
    }

    private void fillReference(Control referenceInput, Object value) {

        referenceInput.shouldExists();

        if (!(referenceInput instanceof N2oInputSelect))
            throw new IllegalArgumentException("control is not check box");

        N2oInputSelect control = (N2oInputSelect) referenceInput;
        control.expandPopUpOptions();
        control.select((int) value);
    }

    private RefBookListPage login() {

        LoginPage loginPage = open("/", LoginPage.class);
        RefBookListPage refBookListPage = loginPage.login(USERNAME, PASSWORD);
        logger.info("User logged in");
        return refBookListPage;
    }

    private void waitActionResult() {
        Selenide.sleep(SLEEP_TIME);
    }

    private RefBook generateRefBook(String type, int rowsCount, List<FieldType> fieldTypes, RefBook referredBook) {

        return new RefBook(
                "a_" + RandomStringUtils.randomAlphabetic(5),
                "a " + RandomStringUtils.randomAlphabetic(5) +
                        " (" + now().format(DATE_TIME_FORMATTER) + ")",
                "shortName",
                "system",
                "description",
                type,
                generateRefBookRows(rowsCount, fieldTypes, referredBook)
        );
    }

    private List<Map<RefBookField, Object>> generateRefBookRows(int rowsCount, List<FieldType> fieldTypes, RefBook referredBook) {

        List<Map<RefBookField, Object>> result = new ArrayList<>(rowsCount);
        for(int i = 0; i < rowsCount; i++) {
            Map<RefBookField, Object> row = new LinkedHashMap<>(fieldTypes.size());
            for (FieldType fieldType : fieldTypes) {
                switch (fieldType) {
                    case STRING -> row.put(
                            new RefBookField("name", ATTR_NAME_NAME, fieldType, false),
                            RandomStringUtils.randomAlphabetic(5)
                    );
                    case INTEGER -> row.put(
                            new RefBookField("id", ATTR_ID_NAME, fieldType, true),
                            String.valueOf(refBookDataIdSeq.getAndIncrement())
                    );
                    case DOUBLE -> row.put(
                            new RefBookField("some_double", "Некоторое дробное поле", fieldType, false),
                            String.valueOf(RandomUtils.nextDouble(1.01, 2)).substring(0, 3)
                    );
                    case DATE -> row.put(
                            new RefBookField("some_date", "Некоторая дата", fieldType, false),
                            LocalDate.now().format(EDIT_FIELD_DATE_FORMATTER)
                    );
                    case BOOLEAN -> row.put(
                            new RefBookField("some_boolean", "Некоторое булеан поле", fieldType,
                                    false), RandomUtils.nextBoolean()
                    );
                    case REFERENCE -> row.put(
                            new RefBookField("some_reference", ATTR_REFERENCE_NAME, fieldType, false,
                                    referredBook, new AbstractMap.SimpleEntry<>(1, ATTR_NAME_NAME)),
                            referredBook != null && referredBook.getRows().size() > i ? i : null
                    );
                    default -> throw new IllegalArgumentException(fieldType + " invalid field type");
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
