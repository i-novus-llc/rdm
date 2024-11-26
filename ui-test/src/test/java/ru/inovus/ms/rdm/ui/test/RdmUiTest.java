package ru.inovus.ms.rdm.ui.test;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import net.n2oapp.framework.autotest.api.component.DropDown;
import net.n2oapp.framework.autotest.api.component.control.Control;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputSelect;
import net.n2oapp.framework.autotest.impl.component.control.N2oSelect;
import net.n2oapp.framework.autotest.run.AutoTestBase;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.SystemProperties;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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

import static java.util.stream.Collectors.toList;
import static ru.inovus.ms.rdm.ui.test.util.UiTestUtil.fillCheckBox;
import static ru.inovus.ms.rdm.ui.test.util.UiTestUtil.fillInputControl;

@ExtendWith(SpringExtension.class)
public class RdmUiTest extends AutoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RdmUiTest.class);

    private static final String DEFAULT_URL = "http://localhost:8080";
    //private static final String USERNAME = "admin";
    //private static final String PASSWORD = "admin";
    private static final String USERNAME = "rdm_admin";
    private static final String PASSWORD = "rdm.admin";

    // Время создания справочника (для локализации ошибки).
    private static final ZoneId UNIVERSAL_TIMEZONE = ZoneId.of("UTC");
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private static final int DATA_ROWS_CREATE_COUNT = 3;
    //private static final int DATA_ROWS_CREATE_COUNT = 1; // debug only
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
    public static void beforeClass() {

        // Временный фикс для повторного бага:
        // https://github.com/SeleniumHQ/selenium/issues/14544
        System.setProperty("selenide.headless", "false");

        configureSelenide();

        final MutableCapabilities capabilities = Configuration.browserCapabilities;
        //capabilities.setCapability("acceptSslCerts", true);
        capabilities.setCapability("acceptInsecureCerts", true);
        //capabilities.setCapability("ignore-certificate-errors", true);

        final ChromeOptions options = new ChromeOptions();
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        options.setCapability(CapabilityType.ENABLE_DOWNLOADS, true);
        options.setExperimentalOption("useAutomationExtension", false);
        options.setExperimentalOption("excludeSwitches",
                new String[] {"enable-automation", "load-extension", "enable-logging"});
        options.addArguments("--disable-blink-features=AutomationControlled");

        capabilities.setCapability(ChromeOptions.CAPABILITY, options);

        //Configuration.browserSize = "1920x1200";
        Configuration.browserSize = "1280x800";

        //Configuration.baseUrl = getAppUrl();
        Configuration.timeout = 8000;
    }

    @AfterAll
    public static void tearDown() {
        Selenide.closeWebDriver();
    }

    /**
     * URL запущенного фронтенда RDM.
     */
    protected static String getAppUrl() {

        final String baseUrl = SystemProperties.get("rdm.url");
        return baseUrl == null ? DEFAULT_URL : baseUrl;
    }

    @Override
    protected String getBaseUrl() {
        return getAppUrl();
    }

    @AfterEach
    public void logout() {
        openPage(LoginPage.class, "/logout").shouldExists();
    }

    //@Test
    //public void testOpen() {
    //    Selenide.open( "https://dzen.ru/");
    //    //Selenide.open( "https://www.google.com/");
    //}

    //@Test
    //public void testAddRefBookData() {
    //
    //    final RefBookListPage refBookListPage = login();
    //    refBookListPage.shouldExists();
    //
    //    final RefBook refBook = new RefBook(
    //            "a_LlOLn",
    //            "a TdEmU (2024-11-22T09:07:17)",
    //            "shortName",
    //            "system",
    //            "description",
    //            RefBook.getUnversionedType(),
    //            generateRows(DATA_ROWS_CREATE_COUNT, DEFAULT_FIELD_TYPES, null)
    //    );
    //
    //    search(refBookListPage, refBook);
    //    refBookListPage.columnCodeShouldHaveText(refBook.getCode());
    //
    //    final RefBookEditPage refBookEditPage = openRefBookEditPage(refBookListPage, 0);
    //    refBookEditPage.shouldExists();
    //
    //    final DataListWidget dataListWidget = refBookEditPage.data();
    //    dataListWidget.shouldExists();
    //    dataListWidget.shouldBeVisible();
    //    // Don't use: dataListWidget.shouldBeEnabled()
    //
    //    final List<String> nameColumnTexts = new ArrayList<>();
    //    for (Map<RefBookField, Object> row : refBook.getRows()) {
    //
    //        final DataRowForm dataForm = dataListWidget.openAddRowForm();
    //        dataForm.shouldExists();
    //
    //        fillDataRowForm(dataForm, row);
    //        dataForm.save();
    //
    //        nameColumnTexts.add(getNameColumnText(row));
    //        dataListWidget.columnShouldHaveTexts(1, nameColumnTexts);
    //    }
    //}

    /**
     * Проверка работы с обычным (версионным) справочником.
     */
    //@Test
    public void testDefaultRefBook() {
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

        final RefBookListPage refBookListPage = login();
        refBookListPage.shouldExists();

        final RefBook refBook = generateRefBook(type, DATA_ROWS_CREATE_COUNT, DEFAULT_FIELD_TYPES, null);

        // Создание.
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
     * Проверка работы с обычным справочником, ссылающимся на обычный справочник.
     */
    //@Test
    public void testDefaultReferrerToDefault() {
        testReferrerRefBook(null, null);
    }

    /**
     * Проверка работы с обычным справочником, ссылающимся на неверсионный справочник.
     */
    //@Test
    public void testDefaultReferrerToUnversioned() {
        testReferrerRefBook(RefBook.getUnversionedType(), null);
    }

    private void testReferrerRefBook(String referredType, String referrerType) {

        final RefBook referredBook = generateRefBook(referredType, DATA_ROWS_CREATE_COUNT, REFERRED_FIELD_TYPES, null);
        final RefBook referrerBook = generateRefBook(referrerType, DATA_ROWS_CREATE_COUNT, REFERRER_FIELD_TYPES, referredBook);

        final RefBookListPage refBookListPage = login();
        refBookListPage.shouldExists();

        // Создание обычного справочника.
        createRefBook(refBookListPage, referredBook);
        refBookListPage.shouldExists();

        search(refBookListPage, referredBook);
        refBookListPage.columnCodeShouldHaveText(referredBook.getCode());

        // Создание ссылочного справочника.
        createRefBook(refBookListPage, referrerBook);

        // Создание конфликтов.
        search(refBookListPage, referredBook);
        refBookListPage.columnCodeShouldHaveText(referredBook.getCode());

        RefBookEditPage refBookEditPage = openRefBookEditPage(refBookListPage, 0);
        createDataConflicts(refBookEditPage, referredBook);

        // Разрешение конфликтов.
        search(refBookListPage, referrerBook);
        refBookListPage.columnCodeShouldHaveText(referrerBook.getCode());

        refBookEditPage = openRefBookEditPage(refBookListPage, 0);
        resolveDataConflicts(refBookEditPage, referrerBook);

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

        final DataListWidget dataListWidget = refBookEditPage.data();
        // Это динамически формируемая таблица.
        // Она не создаётся, если нет записей справочника.
        dataListWidget.shouldNotExists();

        final List<String> nameColumnTexts = new ArrayList<>();
        for (Map<RefBookField, Object> row : refBook.getRows()) {

            final DataRowForm dataForm = dataListWidget.openAddRowForm();
            dataForm.shouldExists();

            fillDataRowForm(dataForm, row);
            dataForm.save();

            nameColumnTexts.add(getNameColumnText(row));
            dataListWidget.columnShouldHaveTexts(1, nameColumnTexts);
        }

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void createStructure(StructureWidget structureWidget, Set<RefBookField> fieldsToFirstRefBook) {

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

    private void editRefBook(RefBookEditPage refBookEditPage, RefBook refBook) {

        final List<Map<RefBookField, Object>> existedRows = refBook.getRows();
        final List<String> nameColumnTexts = existedRows.stream().map(this::getNameColumnText).collect(toList());

        refBookEditPage.shouldExists();
        DataListWidget dataListWidget = refBookEditPage.data();

        final List<Map<RefBookField, Object>> refBookRows = generateRows(1, DEFAULT_FIELD_TYPES, null);
        final Map<RefBookField, Object> row = refBookRows.get(0);

        final DataRowForm addForm = dataListWidget.openAddRowForm();
        fillDataRowForm(addForm, row);
        addForm.save();

        nameColumnTexts.add(getNameColumnText(row));
        dataListWidget.columnShouldHaveTexts(1, nameColumnTexts);

        final int lastRowNum = nameColumnTexts.size() - 1;
        final DataRowForm editForm = dataListWidget.openEditRowForm(lastRowNum);
        final String newNameValue = "Другое наименование";
        fillInputControl(editForm.stringInput(ATTR_NAME_NAME), newNameValue);
        editForm.edit();

        nameColumnTexts.set(lastRowNum, newNameValue);
        dataListWidget.columnShouldHaveTexts(1, nameColumnTexts);

        dataListWidget.deleteRow(lastRowNum);

        nameColumnTexts.remove(lastRowNum);
        dataListWidget.columnShouldHaveTexts(1, nameColumnTexts);

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void createDataConflicts(RefBookEditPage refBookEditPage, RefBook refBook) {

        final List<Map<RefBookField, Object>> existedRows = refBook.getRows();
        final List<String> nameColumnTexts = existedRows.stream().map(this::getNameColumnText).collect(toList());

        refBookEditPage.shouldExists();
        final DataListWidget dataListWidget = refBookEditPage.data();

        // Конфликт DELETED.
        dataListWidget.deleteRow(0);

        nameColumnTexts.remove(0);
        dataListWidget.columnShouldHaveTexts(1, nameColumnTexts);

        // Конфликт UPDATED.
        final DataRowForm editForm = dataListWidget.openEditRowForm(0);
        final String newNameValue = nameColumnTexts.get(0) + "_updated";
        fillInputControl(editForm.stringInput(ATTR_NAME_NAME), newNameValue);
        editForm.edit();

        nameColumnTexts.set(0, newNameValue);
        dataListWidget.columnShouldHaveTexts(1, nameColumnTexts);

        if (refBook.isUnversioned()) {
            openRefBookListPage();
        } else {
            publishRefBook(refBookEditPage);
        }
    }

    private void resolveDataConflicts(RefBookEditPage refBookEditPage, RefBook referrer) {

        final List<Map<RefBookField, Object>> existedRows = referrer.getRows();
        final List<String> nameColumnTexts = existedRows.stream().map(this::getNameColumnText).collect(toList());
        final List<String> conflictedNameColumnTexts = nameColumnTexts.subList(0, 2);

        refBookEditPage.shouldExists();
        //final DataWidget dataWidget = refBookEditPage.data();
        //dataWidget.columnShouldHaveTexts(1, nameColumnTexts);

        DataWithConflictsWidget dataWithConflictsWidget = refBookEditPage.dataWithConflicts();
        dataWithConflictsWidget.rowShouldHaveSize(2); // Два конфликта
        dataWithConflictsWidget.columnShouldHaveTexts(1, conflictedNameColumnTexts);

        refBookEditPage.refreshReferrer();
        waitActionResult(SLEEP_TIME);

        conflictedNameColumnTexts.remove(1);
        dataWithConflictsWidget = refBookEditPage.dataWithConflicts();
        dataWithConflictsWidget.rowShouldHaveSize(1); // Один конфликт
        dataWithConflictsWidget.columnShouldHaveTexts(1, conflictedNameColumnTexts);

        final DataRowForm editForm = dataWithConflictsWidget.fixRowForm(0);
        fillReference(editForm.referenceInput(ATTR_REFERENCE_NAME), 0);
        editForm.edit();

        dataWithConflictsWidget = refBookEditPage.dataWithConflicts();
        dataWithConflictsWidget.rowShouldHaveSize(0); // Нет конфликтов

        openRefBookListPage();
    }

    private void openRefBookListPage() {

        openPage(RefBookListPage.class, "/");
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

    private void fillAttributeForm(AttributeForm form, RefBookField refBookField) {

        fillInputControl(form.codeInput(), refBookField.getCode());
        fillInputControl(form.nameInput(), refBookField.getName());

        final N2oSelect typeInput = form.typeInput();
        typeInput.shouldBeEmpty();
        fillInputControl(typeInput, refBookField.getType().getLabel());

        if (refBookField.isReferenceType()) {

            final N2oInputSelect refBookInput = form.refBookInput();
            refBookInput.shouldBeEmpty();
            refBookInput.setValue(refBookField.getReferredBook().getCode());
            refBookInput.openPopup();
            refBookInput.setValue(refBookField.getReferredBook().getCode());
            refBookInput.shouldHaveValue(refBookField.getReferredBook().getCode());

            final N2oInputSelect displayAttrInput = form.displayAttrInput();
            final Map.Entry<Integer, String> referredField = refBookField.getReferredField();
            if (referredField != null) {
                //final DropDown typeDropDown = displayAttrInput.dropdown();
                //typeDropDown.selectItem(referredField.getKey());
                //displayAttrInput.closePopup();
                displayAttrInput.setValue(referredField.getValue());
                displayAttrInput.shouldHaveValue(referredField.getValue());
            }
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
            fillInputControl(typeInput, refBook.getType());
            //typeInput.shouldBeEmpty();
            //typeInput.setValue(refBook.getType());
            //typeInput.openPopup();
            //typeInput.setValue(refBook.getType());
            //typeInput.closePopup();
            //typeInput.shouldHaveValue(refBook.getType());
        }
    }

    private void fillReference(Control referenceInput, Object value) {

        referenceInput.shouldExists();

        if (!(referenceInput instanceof N2oInputSelect control))
            throw new IllegalArgumentException("Control is not reference input");

        // to-do: Очищать только при наличии значения!
        control.clear();
        waitActionResult(SLEEP_TIME / 2);
        
        final DropDown dropDown = control.dropdown();
        dropDown.selectItem((int) value);
        //control.select((int) value);
    }

    private RefBookListPage login() {

        logger.info("User log in");
        final LoginPage loginPage = openPage(LoginPage.class, "/");
        final RefBookListPage refBookListPage = loginPage.login(USERNAME, PASSWORD);
        logger.info("User logged in");

        //logger.info("Main page");
        //final RefBookListPage refBookListPage = openPage(RefBookListPage.class, "/");
        return refBookListPage;
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

    /**
     * Формирование информации о записях справочника.
     */
    private List<Map<RefBookField, Object>> generateRows(int rowCount,
                                                         List<FieldType> fieldTypes,
                                                         RefBook referredBook) {

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

    private String getNameColumnText(Map<RefBookField, Object> row) {

        final Map.Entry<RefBookField, Object> field = row.entrySet().stream()
                .filter(entry -> "name".equals(entry.getKey().getCode()))
                .findAny().orElse(null);

        return (field != null) ? (String) field.getValue() : null;
    }

    /**
     * Открытие страницы с url без query-параметров.
     *
     * @param clazz   класс страницы
     * @param pageUrl url страницы
     * @return Страница
     */
    private <T extends Page> T openPage(Class<T> clazz, String pageUrl) {
        return open(clazz, pageUrl, null);
    }

    /**
     * Ожидание результата действия.
     *
     * @param milliseconds Время ожидания (мс)
     */
    private void waitActionResult(long milliseconds) {

        Selenide.sleep(milliseconds != 0 ? milliseconds : 1);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(UNIVERSAL_TIMEZONE);
    }
}
