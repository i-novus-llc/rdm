package ru.inovus.ms.rdm.ui.test;

import com.codeborne.selenide.Selenide;
import lombok.extern.slf4j.Slf4j;
import net.n2oapp.framework.autotest.api.component.DropDown;
import net.n2oapp.framework.autotest.api.component.control.Control;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputSelect;
import net.n2oapp.framework.autotest.impl.component.control.N2oSelect;
import net.n2oapp.framework.autotest.run.AutoTestBase;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.system.SystemProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.inovus.ms.rdm.ui.test.model.FieldType;
import ru.inovus.ms.rdm.ui.test.model.RefBook;
import ru.inovus.ms.rdm.ui.test.model.RefBookField;
import ru.inovus.ms.rdm.ui.test.page.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.ui.test.util.UiTestUtil.*;

/**
 * Общий абстрактный класс для тестов RDM UI.
 * <p>
 * Для корректной работы необходимо указание значений настроек:
 * selenide.baseUrl - базовый url для rdm ui
 * rdm.username - логин пользователя rdm с администраторскими правами
 * rdm.password - пароль пользователя с логином rdm.username
 * <p>
 * Пример:
 * <p>- для VM options:
 * <pre>
 * -Dselenide.baseUrl=http://localhost:8080 -Drdm.username=rdm_admin -Drdm.password=rdm.admin
 * </pre>
 * <p>- для переменных окружения:
 * <pre>
 * selenide.baseUrl=http://localhost:8080;rdm.username=rdm_admin;rdm.password=rdm.admin
 * </pre>
 * <p>
 * В случае, если при запуске тестов падает без открытия страницы,
 * надо применить временный фикс бага с white-page: https://github.com/SeleniumHQ/selenium/issues/14544
 * -Dselenide.headless=false
 */
@Slf4j
@ExtendWith(SpringExtension.class)
abstract class AbstractRdmUiTest extends AutoTestBase {

    static final String BASE_URL = SystemProperties.get("selenide.baseUrl");
    static final String USERNAME = SystemProperties.get("rdm.username");
    static final String PASSWORD = SystemProperties.get("rdm.password");

    // Время создания справочника (для локализации ошибки).
    static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    static final int DATA_ROWS_COUNT = 3;
    //static final int DATA_ROWS_COUNT = 1; // debug only

    static final long LOGIN_PAGE_WAIT_TIME = TimeUnit.SECONDS.toMillis(1);
    static final long REFERRED_ATTRIBUTE_LOAD_WAIT_TIME = TimeUnit.SECONDS.toMillis(2);
    static final long REFBOOK_PUBLISH_WAIT_TIME = TimeUnit.SECONDS.toMillis(6);
    static final long CONFLICTS_RESOLVE_WAIT_TIME = TimeUnit.SECONDS.toMillis(6);
    static final long INPUT_SELECT_CLEAR_WAIT_TIME = TimeUnit.SECONDS.toMillis(2);

    // Все простые поля для проверки справочников.
    static final List<FieldType> DEFAULT_FIELD_TYPES = List.of(
            // В списке дата всегда должна быть последней, иначе календарь перекрывает другие поля.
            FieldType.INTEGER, FieldType.STRING, FieldType.DOUBLE, FieldType.BOOLEAN, FieldType.DATE
    );

    // Минимальное количество полей для проверки ссылочных справочников.
    static final List<FieldType> REFERRED_FIELD_TYPES = List.of(
            FieldType.INTEGER, FieldType.STRING
    );
    static final List<FieldType> REFERRER_FIELD_TYPES = List.of(
            // STRING нужен, иначе вылетает по таймауту.
            FieldType.INTEGER, FieldType.STRING, FieldType.REFERENCE
    );

    // Наименования атрибутов/полей.
    static final String ATTR_ID_NAME = "Идентификатор";
    static final String ATTR_NAME_NAME = "Наименование";
    static final String ATTR_REFERENCE_NAME = "Ссылочное поле";

    // Формат поля даты при редактировании значения.
    static final String EDIT_FIELD_DATE_PATTERN = "dd.MM.yyyy";
    static final DateTimeFormatter EDIT_FIELD_DATE_FORMATTER = DateTimeFormatter.ofPattern(EDIT_FIELD_DATE_PATTERN);

    // Легче работать с упорядоченным по id списком, поэтому через этот класс задаём порядок.
    private final AtomicInteger refBookDataIdSeq = new AtomicInteger();

    @BeforeAll
    public static void beforeClass() {

        log.debug("Start configure Selenide");
        configureSelenide();
        log.debug("Finish configure Selenide");
    }

    @AfterAll
    public static void afterClass() {

        Selenide.closeWebDriver();
        log.debug("Web driver is closed");
    }

    /**
     * URL запущенного фронтенда RDM.
     */
    @Override
    protected String getBaseUrl() {
        return BASE_URL;
    }

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void cleanUp() {
        logout();
    }

    /**
     * Проверка работы со справочником указанного типа.
     *
     * @param refBookListPage страница 'Список справочников'
     * @param type            тип справочника
     */
    protected void testRefBook(RefBookListPage refBookListPage, String type) {

        // Создание.
        final RefBook refBook = generateRefBook(type, DATA_ROWS_COUNT, DEFAULT_FIELD_TYPES, null);
        createRefBook(refBookListPage, refBook);
        refBookListPage.shouldExists();

        search(refBookListPage, refBook);
        refBookListPage.columnCodeShouldHaveText(refBook.getCode());

        // Редактирование.
        final RefBookEditPage refBookEditPage = openRefBookEditPage(refBookListPage, 0);
        editRefBook(refBookEditPage, refBook);

        search(refBookListPage, refBook);
        refBookListPage.columnCodeShouldHaveText(refBook.getCode());

        // Удаление.
        refBookListPage.deleteRow(0);

        search(refBookListPage, refBook);
        refBookListPage.rowShouldHaveSize(0);
    }

    /**
     * Проверка работы со справочником, который ссылается на другой справочник.
     *
     * @param refBookListPage страница 'Список справочников'
     * @param referredType    тип справочника, на который ссылаемся (исходный)
     * @param referrerType    тип справочника, который ссылается (ссылочный)
     */
    @SuppressWarnings("SameParameterValue")
    protected void testReferrerRefBook(RefBookListPage refBookListPage, String referredType, String referrerType) {

        // Создание обычного справочника.
        final RefBook referredBook = generateRefBook(referredType, DATA_ROWS_COUNT, REFERRED_FIELD_TYPES, null);
        createRefBook(refBookListPage, referredBook);
        refBookListPage.shouldExists();

        search(refBookListPage, referredBook);
        refBookListPage.columnCodeShouldHaveText(referredBook.getCode());

        // Создание ссылочного справочника.
        final RefBook referrerBook = generateRefBook(referrerType, DATA_ROWS_COUNT, REFERRER_FIELD_TYPES, referredBook);
        createRefBook(refBookListPage, referrerBook);

        // Создание конфликтов.
        search(refBookListPage, referredBook);
        refBookListPage.columnCodeShouldHaveText(referredBook.getCode());

        final RefBookEditPage referredBookEditPage = openRefBookEditPage(refBookListPage, 0);
        createDataConflicts(referredBookEditPage, referredBook);

        // Разрешение конфликтов.
        search(refBookListPage, referrerBook);
        refBookListPage.columnCodeShouldHaveText(referrerBook.getCode());

        final RefBookEditPage referrerBookEditPage = openRefBookEditPage(refBookListPage, 0);
        resolveDataConflicts(referrerBookEditPage, referrerBook);

        // Удаление ссылочного справочника.
        search(refBookListPage, referrerBook);
        refBookListPage.columnCodeShouldHaveText(referrerBook.getCode());

        refBookListPage.deleteRow(0);
        search(refBookListPage, referrerBook);
        refBookListPage.rowShouldHaveSize(0);

        // Удаление обычного справочника.
        search(refBookListPage, referredBook);
        refBookListPage.columnCodeShouldHaveText(referredBook.getCode());

        refBookListPage.deleteRow(0);
        search(refBookListPage, referredBook);
        refBookListPage.rowShouldHaveSize(0);
    }

    void search(RefBookListPage refBookListPage, RefBook refBook) {

        fillInputControl(refBookListPage.codeFilter(), refBook.getCode());
        fillInputControl(refBookListPage.nameFilter(), refBook.getName());
        refBookListPage.shouldExists();

        refBookListPage.search();
    }

    void createRefBook(RefBookListPage refBookListPage, RefBook refBook) {

        final CreateRefBookWidget createRefBookWidget = refBookListPage.openCreateRefBookPage();
        createRefBookWidget.shouldExists();

        fillRefBookWidget(createRefBookWidget, refBook);
        final RefBookEditPage refBookEditPage = createRefBookWidget.save();
        refBookEditPage.shouldExists();

        final StructureWidget structureWidget = refBookEditPage.structure();
        structureWidget.shouldExists();

        final Set<RefBookField> fieldsToFirstRefBook = refBook.getRows().get(0).keySet();
        createStructure(structureWidget, fieldsToFirstRefBook);

        final DataTableWidget dataTableWidget = refBookEditPage.dataTable();
        dataTableWidget.shouldExists();
        dataTableWidget.shouldBeVisible();
        // Don't use: dataTableWidget.shouldBeEnabled()

        final List<String> nameColumnTexts = new ArrayList<>(refBook.getRows().size());
        for (Map<RefBookField, Object> row : refBook.getRows()) {

            final DataRowForm dataForm = dataTableWidget.openAddRowForm();
            dataForm.shouldExists();

            fillDataRowForm(dataForm, row);
            dataForm.save();

            nameColumnTexts.add(getNameColumnText(row));
            dataTableWidget.columnShouldHaveTexts(1, nameColumnTexts);
        }

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    void createStructure(StructureWidget structureWidget, Set<RefBookField> fieldsToFirstRefBook) {

        final List<String> codeColumnTexts = new ArrayList<>(fieldsToFirstRefBook.size());
        final List<String> nameColumnTexts = new ArrayList<>(fieldsToFirstRefBook.size());

        fieldsToFirstRefBook.forEach(field -> {

            final AttributeForm attributeForm = structureWidget.openAddForm();
            attributeForm.shouldExists();

            fillAttributeForm(attributeForm, field);
            attributeForm.save();

            codeColumnTexts.add(field.getCode());
            nameColumnTexts.add(field.getName());

            structureWidget.columnShouldHaveTexts(1, codeColumnTexts);
            structureWidget.columnShouldHaveTexts(2, nameColumnTexts);
        });
    }

    void editRefBook(RefBookEditPage refBookEditPage, RefBook refBook) {

        final List<Map<RefBookField, Object>> existedRows = refBook.getRows();
        final List<String> nameColumnTexts = existedRows.stream().map(this::getNameColumnText).collect(toList());

        refBookEditPage.shouldExists();
        final DataTableWidget dataTableWidget = refBookEditPage.dataTable();

        final List<Map<RefBookField, Object>> refBookRows = generateRows(1, DEFAULT_FIELD_TYPES, null);
        final Map<RefBookField, Object> row = refBookRows.get(0);

        final DataRowForm addForm = dataTableWidget.openAddRowForm();
        fillDataRowForm(addForm, row);
        addForm.save();

        nameColumnTexts.add(getNameColumnText(row));
        dataTableWidget.columnShouldHaveTexts(1, nameColumnTexts);

        final int lastRowNum = nameColumnTexts.size() - 1;
        final DataRowForm editForm = dataTableWidget.openEditRowForm(lastRowNum);
        final String newNameValue = "Другое наименование";
        fillInputControl(editForm.stringInput(ATTR_NAME_NAME), newNameValue);
        editForm.edit();

        nameColumnTexts.set(lastRowNum, newNameValue);
        dataTableWidget.columnShouldHaveTexts(1, nameColumnTexts);

        dataTableWidget.deleteRow(lastRowNum);

        nameColumnTexts.remove(lastRowNum);
        dataTableWidget.columnShouldHaveTexts(1, nameColumnTexts);

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    void createDataConflicts(RefBookEditPage refBookEditPage, RefBook refBook) {

        refBookEditPage.shouldExists();

        final List<Map<RefBookField, Object>> existedRows = refBook.getRows();
        final List<String> nameColumnTexts = existedRows.stream().map(this::getNameColumnText).collect(toList());

        final DataTableWidget dataTableWidget = refBookEditPage.dataTable();

        // Конфликт DELETED.
        dataTableWidget.deleteRow(0);

        nameColumnTexts.remove(0);
        dataTableWidget.columnShouldHaveTexts(1, nameColumnTexts);

        // Конфликт UPDATED.
        final DataRowForm editForm = dataTableWidget.openEditRowForm(0);
        final String newNameValue = nameColumnTexts.get(0) + "_updated";
        fillInputControl(editForm.stringInput(ATTR_NAME_NAME), newNameValue);
        editForm.edit();

        nameColumnTexts.set(0, newNameValue);
        dataTableWidget.columnShouldHaveTexts(1, nameColumnTexts);

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    void resolveDataConflicts(RefBookEditPage refBookEditPage, RefBook referrer) {

        refBookEditPage.shouldExists();

        final List<Map<RefBookField, Object>> existedRows = referrer.getRows();
        final List<String> nameColumnTexts = existedRows.stream().map(this::getNameColumnText).collect(toList());
        final List<String> conflictedNameColumnTexts = nameColumnTexts.subList(0, 2);

        //final DataWidget dataWidget = refBookEditPage.data();
        //dataWidget.columnShouldHaveTexts(1, nameColumnTexts);

        final DataTableWithConflictsWidget widget2 = refBookEditPage.dataTableWithConflicts();
        widget2.rowShouldHaveSize(2); // Два конфликта
        widget2.columnShouldHaveTexts(1, conflictedNameColumnTexts);

        refBookEditPage.refreshReferrer();
        waitActionResult(CONFLICTS_RESOLVE_WAIT_TIME);

        conflictedNameColumnTexts.remove(1);
        final DataTableWithConflictsWidget widget1 = refBookEditPage.dataTableWithConflicts();
        widget1.rowShouldHaveSize(1); // Один конфликт
        widget1.columnShouldHaveTexts(1, conflictedNameColumnTexts);

        final DataRowForm editForm = widget1.fixRowForm(0);
        fillReference(editForm.referenceInput(ATTR_REFERENCE_NAME), 0);
        editForm.edit();

        final DataTableWithConflictsWidget widget0 = refBookEditPage.dataTableWithConflicts();
        widget0.rowShouldHaveSize(0); // Нет конфликтов

        openRefBookListPage();
    }

    void openRefBookListPage() {
        openPage(RefBookListPage.class, BASE_URL + "/");
    }

    RefBookEditPage openRefBookEditPage(RefBookListPage refBookListPage, int rowNum) {
        return refBookListPage.openRefBookEditPage(rowNum);
    }

    void publishRefBook(RefBookEditPage refBookEditPage) {

        refBookEditPage.publish();
        waitActionResult(REFBOOK_PUBLISH_WAIT_TIME);
        }

    /**
     * Формирование информации о справочнике для создания.
     *
     * @param type         тип справочника
     * @param rowCount     количество создаваемых записей
     * @param fieldTypes   типы полей записи / атрибутов справочника
     * @param referredBook справочник, на который ссылаются
     * @return Информация о справочнике
     */
    @SuppressWarnings("SameParameterValue")
    RefBook generateRefBook(String type, int rowCount, List<FieldType> fieldTypes, RefBook referredBook) {

        return new RefBook(
                "a_a" + RandomStringUtils.randomAlphabetic(5).toUpperCase(),
                "a " + RandomStringUtils.randomAlphabetic(5) +
                        (referredBook != null ? " to " + referredBook.getCode() : "") +
                        " (" + nowUtc().format(DATE_TIME_FORMATTER) + ")",
                "shortName",
                "system",
                "description",
                type,
                generateRows(rowCount, fieldTypes, referredBook)
        );
    }

    /**
     * Формирование информации о записях справочника.
     *
     * @param rowCount     количество записей
     * @param fieldTypes   типы полей записи / атрибутов справочника
     * @param referredBook справочник, на который ссылаются
     * @return Информация в виде списка записей
     */
    List<Map<RefBookField, Object>> generateRows(
            int rowCount,
                                                         List<FieldType> fieldTypes,
            RefBook referredBook
    ) {
        final List<Map<RefBookField, Object>> result = new ArrayList<>(rowCount);
        for(int i = 0; i < rowCount; i++) {
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
                            new RefBookField("some_double", "Некоторая дробное число", fieldType, false),
                            String.valueOf(RandomUtils.nextDouble(1.01, 2)).substring(0, 3)
                    );
                    case DATE -> row.put(
                            new RefBookField("some_date", "Некоторая дата", fieldType, false),
                            LocalDate.now().format(EDIT_FIELD_DATE_FORMATTER)
                    );
                    case BOOLEAN -> row.put(
                            new RefBookField("some_boolean", "Некоторое логическое поле", fieldType, false),
                            RandomUtils.nextBoolean()
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

    void fillRefBookWidget(CreateRefBookWidget widget, RefBook refBook) {

        fillInputControl(widget.codeInput(), refBook.getCode());
        fillInputControl(widget.nameInput(), refBook.getName());
        fillInputControl(widget.shortNameInput(), refBook.getShortName());
        fillInputControl(widget.descriptionInput(), refBook.getDescription());

        if (refBook.getType() != null) {
            final N2oInputSelect typeInput = widget.typeInput();
            fillInputControl(typeInput, refBook.getType());
        }
    }

    void fillAttributeForm(AttributeForm form, RefBookField refBookField) {

        fillInputControl(form.codeInput(), refBookField.getCode());
        fillInputControl(form.nameInput(), refBookField.getName());

        final N2oSelect typeInput = form.typeInput();
        typeInput.shouldBeEmpty();
        fillInputControl(typeInput, refBookField.getType().getLabel());

        if (refBookField.isReferenceType()) {

            final N2oInputSelect refBookInput = form.refBookInput();
            refBookInput.shouldBeEmpty();

            final String referredBookCode = refBookField.getReferredBook().getCode();
            refBookInput.setValue(referredBookCode);
            fillN2oInputSelectValue(refBookInput, referredBookCode);
            waitActionResult(REFERRED_ATTRIBUTE_LOAD_WAIT_TIME); // Ожидание для подгрузки атрибутов

            final Map.Entry<Integer, String> referredField = refBookField.getReferredField();
            if (referredField != null) {

                final N2oInputSelect displayAttrInput = form.displayAttrInput();
                displayAttrInput.shouldBeEmpty();
                displayAttrInput.setValue(referredField.getValue());
                fillN2oInputSelectValue(displayAttrInput, referredField.getValue());
            }
        }

        if (refBookField.isPrimary()) {
            fillCheckBox(form.primaryKeyInput(), true);
        }
    }

    void fillDataRowForm(DataRowForm form, Map<RefBookField, Object> row) {

        row.forEach((key, value) -> {
            switch (key.getType()) {
                case STRING -> fillInputControl(form.stringInput(key.getName()), value.toString());
                case INTEGER -> fillInputControl(form.integerInput(key.getName()), value.toString());
                case DOUBLE -> fillInputControl(form.doubleInput(key.getName()), value.toString());
                case DATE -> fillInputControl(form.dateInput(key.getName()), (value.toString()));
                case BOOLEAN -> fillCheckBox(form.booleanInput(key.getName()), true);
                case REFERENCE -> fillReference(form.referenceInput(key.getName()), value);
                default -> throw new IllegalArgumentException(key.getType() + " invalid field type");
            }
        });
    }

    void fillReference(Control referenceInput, Object value) {

        if (!(referenceInput instanceof N2oInputSelect control))
            throw new IllegalArgumentException("Control is not reference input");

        referenceInput.shouldExists();

        // to-do: Очищать только при наличии значения!
        control.clear();
        waitActionResult(INPUT_SELECT_CLEAR_WAIT_TIME);

        control.openPopup();
        final DropDown dropDown = control.dropdown();
        dropDown.selectItem((int) value);
        control.closePopup();
    }

    /**
     * Получение строкового значения колонки name для записи row.
     *
     * @param row запись
     * @return Строковое значение колонки
     */
    String getNameColumnText(Map<RefBookField, Object> row) {

        final Map.Entry<RefBookField, Object> field = row.entrySet().stream()
                        .filter(entry -> "name".equals(entry.getKey().getCode()))
                        .findAny().orElse(null);

        return (field != null) ? (String) field.getValue() : null;
    }

    RefBookListPage login() {

        waitActionResult(LOGIN_PAGE_WAIT_TIME);

        log.debug("User log in");
        final RdmLoginPage loginPage = open(RdmLoginPage.class); // with logs
        loginPage.shouldExists();

        waitActionResult(LOGIN_PAGE_WAIT_TIME);

        final RefBookListPage refBookListPage = loginPage.login(USERNAME, PASSWORD);
        refBookListPage.shouldExists();
        log.debug("User logged in");

        //log.info("Main page");
        //final RefBookListPage refBookListPage = openPage(RefBookListPage.class, DEFAULT_URL + "/");
        return refBookListPage;
    }

    void logout() {

        log.debug("User log out");
        final RdmLoginPage loginPage = openPage(RdmLoginPage.class,  BASE_URL + "/logout");
        loginPage.shouldExists();
        log.debug("User logged out");
    }

    void runUiTest(Consumer<RefBookListPage> uiTestConsumer) {

        final RefBookListPage refBookListPage = login();
        try {
            uiTestConsumer.accept(refBookListPage);

        } finally {
            logout();
        }
    }
}
