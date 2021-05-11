package ru.inovus.ms.rdm.ui.test;


import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import net.n2oapp.framework.autotest.api.collection.Cells;
import net.n2oapp.framework.autotest.api.collection.Fields;
import net.n2oapp.framework.autotest.api.collection.Toolbar;
import net.n2oapp.framework.autotest.api.component.field.StandardField;
import net.n2oapp.framework.autotest.api.component.page.Page;
import net.n2oapp.framework.autotest.api.component.region.TabsRegion;
import net.n2oapp.framework.autotest.api.component.widget.table.TableWidget;
import net.n2oapp.framework.autotest.impl.component.button.N2oDropdownButton;
import net.n2oapp.framework.autotest.impl.component.control.*;
import net.n2oapp.framework.autotest.impl.component.page.N2oPage;
import net.n2oapp.framework.autotest.impl.component.page.N2oSimplePage;
import net.n2oapp.framework.autotest.impl.component.page.N2oStandardPage;
import net.n2oapp.framework.autotest.impl.component.region.N2oTabsRegion;
import net.n2oapp.framework.autotest.impl.component.widget.N2oFormWidget;
import net.n2oapp.framework.autotest.impl.component.widget.table.N2oTableWidget;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.SystemProperties;
import ru.inovus.ms.rdm.ui.test.custom.component.form.ModalFormWidget;
import ru.inovus.ms.rdm.ui.test.custom.component.page.LoginPage;
import ru.inovus.ms.rdm.ui.test.custom.wrapper.StandardButtonWrapper;
import ru.inovus.ms.rdm.ui.test.custom.wrapper.TableWidgetWrapper;
import ru.inovus.ms.rdm.ui.test.custom.wrapper.TabsRegionWrapper;
import ru.inovus.ms.rdm.ui.test.model.FieldType;
import ru.inovus.ms.rdm.ui.test.model.RefBook;
import ru.inovus.ms.rdm.ui.test.model.RefBookField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.n2oapp.framework.autotest.N2oSelenide.open;
import static net.n2oapp.framework.autotest.N2oSelenide.page;

public class RdmSmokeTest {

    private static final Logger logger = LoggerFactory.getLogger(RdmSmokeTest.class);

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private static final long WAIT_TIME = TimeUnit.SECONDS.toMillis(6);
    private static final long SLEEP_TIME = TimeUnit.SECONDS.toMillis(6);
    private static final int REF_BOOK_CREATED_COUNT = 2;
    private static final int REF_BOOK_DATA_ROWS_CREATE_COUNT = 3;

    private static RefBook firstRefBook;
    private static RefBook secondRefBook;
    private static List<RefBookField> fieldsToFirstRefBook;
    private static List<RefBookField> fieldsToSecondRefBook;

    @BeforeClass
    public static void setUp() {
        Configuration.baseUrl = getBaseUrl();
        firstRefBook = getRefBook();
        secondRefBook = getRefBook();
        fieldsToFirstRefBook = getRefBookFields(true);
        fieldsToSecondRefBook = getRefBookFields(false);
    }

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

    @Test
    public void testRdmPage() {
        login();

        rdmPageShouldExists();

        createRefBook(firstRefBook);
        fillDataToRefBook(fieldsToFirstRefBook);
        publishRefBook();
        waitPublishing();
        editRefBook(firstRefBook);
        publishRefBook();

        createRefBook(secondRefBook);
        fillDataToRefBook(fieldsToSecondRefBook);
        publishRefBook();

        deleteRefBooks();
    }

    private void deleteRefBooks() {
        open("/", N2oSimplePage.class);

        for (int i = 0; i < REF_BOOK_CREATED_COUNT; i++) {
            Selenide.sleep(SLEEP_TIME);
            TableWidgetWrapper table = getTableWidget();
            StandardButtonWrapper deleteButton = getButton(table.toolbar().topLeft(), "Удалить справочник");
            Cells row = table.columns().rows().row(1);
            deleteRow(deleteButton, row);
        }
    }

    private void editRefBook(RefBook refBook) {
        searchRefBook(refBook);

        editRefBookDataRows();

        getTableWidget().waitUntilTableContentLoaded(Condition.enabled, WAIT_TIME);
    }

    private void searchRefBook(RefBook refBook) {
        TableWidgetWrapper tableWidget = getTableWidget();
        tableWidget.waitUntilFilterVisible(Condition.visible, WAIT_TIME);

        TableWidget.Filters filters = tableWidget.filters();

        Fields searchFilterFields = filters.fields();
        searchFilterFields.field("Название справочника").control(N2oInputText.class).val(refBook.getName());
        searchFilterFields.field("Код справочника").control(N2oInputText.class).val(refBook.getCode());

        filters.search();
    }

    private void deleteRow(StandardButtonWrapper button, Cells cells) {
        cells.click();
        button.click();

        Page.Dialog deleteDialog = page(N2oSimplePage.class).dialog("Удалить");
        deleteDialog.shouldBeVisible();
        deleteDialog.click("Да");

        button.waitUntil(Condition.visible, WAIT_TIME);
    }

    private void fillDataToRefBook(List<RefBookField> refBookFields) {
        TabsRegionWrapper refBookTabsRegion = new TabsRegionWrapper(page(N2oStandardPage.class)
                .regions()
                .region(Condition.cssClass("n2o-tabs-region"), N2oTabsRegion.class));

        refBookTabsRegion.waitUntil(Condition.visible, WAIT_TIME);

        createRefBookFields(refBookFields);

        createRefBookDataRows(refBookFields);
    }

    private void editRefBookDataRows() {
        TableWidgetWrapper refBooksTable = getTableWidget();

        refBooksTable.columns().rows().row(0)
                .click();
        refBooksTable.toolbar().topLeft().button("Изменить справочник")
                .click();

        N2oTabsRegion editRefBookTabsRegion = page(N2oStandardPage.class)
                .regions().region(Condition.cssClass("n2o-tabs-region"), N2oTabsRegion.class);

        TabsRegion.TabItem refBookDataTab = editRefBookTabsRegion.tab(Condition.text("Данные"));
        refBookDataTab.click();

        N2oTableWidget refBookDataTable = refBookDataTab.content().widget(N2oTableWidget.class);
        Toolbar refBookDataToolbar = refBookDataTable.toolbar().topRight();

        createRow(getButton(refBookDataToolbar, "Добавить"), fieldsToFirstRefBook);

        TableWidget.Rows rows = refBookDataTable.columns().rows();

        editRow(getButton(refBookDataToolbar, "Изменить"), rows.row(2));

        deleteRow(getButton(refBookDataToolbar, "Удалить"), rows.row(3));

        Selenide.sleep(SLEEP_TIME);
    }

    private void editRow(StandardButtonWrapper button, Cells row) {
        row.click();
        button.click();

        fillFields(fieldsToFirstRefBook);

        ModalFormWidget modalForm = page(N2oSimplePage.class).widget(ModalFormWidget.class);
        modalForm.save("Изменить");

        modalForm.waitUntil(Condition.not(Condition.visible), WAIT_TIME);
    }

    private void publishRefBook() {
        N2oSimplePage n2oSimplePage = page(N2oSimplePage.class);

        N2oDropdownButton n2oDropdownButton = n2oSimplePage.widget(N2oFormWidget.class)
                .toolbar().bottomLeft()
                .button("Действия", N2oDropdownButton.class);

        n2oDropdownButton.shouldBeVisible();

        n2oDropdownButton.click();
        n2oDropdownButton
                .menuItem("Опубликовать").click();

        Page.Dialog publishDialog = n2oSimplePage.dialog("Публикация справочника");
        publishDialog.shouldBeVisible();
        n2oSimplePage.dialog("Публикация справочника").click("Опубликовать");
    }

    private void createRefBookDataRows(List<RefBookField> refBookFields) {
        ModalFormWidget modalForm = page(N2oSimplePage.class).widget(ModalFormWidget.class);
        modalForm.waitUntil(Condition.not(Condition.visible), WAIT_TIME);

        N2oTabsRegion tabsRegion = getTabsRegion();

        TabsRegion.TabItem dataTab = tabsRegion.tab(Condition.text("Данные"));
        dataTab.shouldExists();
        dataTab.click();
        dataTab.shouldBeActive();

        N2oTableWidget refBookDataEditTable = dataTab.content().widget(N2oTableWidget.class);

        for (int i = 0; i < REF_BOOK_DATA_ROWS_CREATE_COUNT; i++) {
            createRow(getButton(refBookDataEditTable.toolbar().topRight(), "Добавить"), refBookFields);
        }
    }

    private void createRow(StandardButtonWrapper button, List<RefBookField> refBookFields) {
        ModalFormWidget modalForm = page(N2oSimplePage.class).widget(ModalFormWidget.class);

        button.click();
        fillFields(refBookFields);

        modalForm.save("Сохранить");
        modalForm.waitUntil(Condition.not(Condition.visible), WAIT_TIME);
    }

    private void fillFields(List<RefBookField> refBookFields) {
        ModalFormWidget modalForm = page(N2oSimplePage.class).widget(ModalFormWidget.class);
        for (RefBookField refBookField : refBookFields) {
            Fields refBookDataFields = modalForm.fields();
            StandardField field = refBookDataFields.field(refBookField.getName());
            switch (refBookField.getAttributeTypeName()) {
                case STRING: {
                    field.control(N2oInputText.class).val(RandomStringUtils.randomAlphabetic(5));
                    break;
                }
                case INTEGER: {
                    field.control(N2oInputText.class).val(String.valueOf(RandomUtils.nextInt()));
                    break;
                }
                case DOUBLE: {
                    field.control(N2oInputText.class).val(String.valueOf(RandomUtils.nextDouble()));
                    break;
                }
                case DATE: {
                    field.control(N2oDateInput.class).val(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    break;
                }
                case BOOLEAN: {
                    field.control(N2oCheckbox.class).setChecked(RandomUtils.nextBoolean());
                    break;
                }
                case LINKED: {
                    N2oInputSelect control = field.control(N2oInputSelect.class);
                    control.expandPopUpOptions();
                    control.select(0);
                    break;
                }
            }
        }
    }

    private void createRefBookFields(List<RefBookField> refBookFields) {
        TabsRegion.TabItem tabItem = getTabsRegion().tab(Condition.text("Структура"));

        tabItem.shouldExists();
        tabItem.click();

        N2oTableWidget refBookStructureEditTable = tabItem.content().widget(N2oTableWidget.class);

        ModalFormWidget modalForm = page(N2oSimplePage.class).widget(ModalFormWidget.class);

        for (RefBookField refBookField : refBookFields) {
            refBookStructureEditTable.toolbar()
                    .bottomRight()
                    .button("Добавить")
                    .click();

            Fields structureModalFormFields = modalForm.fields();
            structureModalFormFields.field("Код").control(N2oInputText.class).val(refBookField.getCode());
            structureModalFormFields.field("Наименование").control(N2oInputText.class).val(refBookField.getName());

            if (refBookField.getAttributeTypeName().equals(FieldType.LINKED)) {
                structureModalFormFields.field("Тип").control(N2oSelect.class)
                        .select(Condition.text(refBookField.getAttributeTypeName().getTranslated()));

                N2oInputSelect selectRefBook = structureModalFormFields.field("Выбор справочника").control(N2oInputSelect.class);
                selectRefBook.expandPopUpOptions();
                selectRefBook.select(Condition.text(firstRefBook.getCode()));

                structureModalFormFields.field("Отображаемый атрибут").control(N2oInputSelect.class).select(0);
            } else {
                structureModalFormFields.field("Тип").control(N2oSelect.class)
                        .select(Condition.text(refBookField.getAttributeTypeName().getTranslated()));
            }

            if (refBookField.isPrimaryKey()) {
                structureModalFormFields.field("Первичный ключ").control(N2oCheckbox.class).setChecked(true);
            }

            modalForm.save("Сохранить");

            modalForm.waitUntil(Condition.not(Condition.visible), WAIT_TIME);

            logger.info("Add ref book field with name: \"{}\", type: \"{}\" success", refBookField.getName(),
                    refBookField.getAttributeTypeName().getTranslated());
        }
    }

    private void createRefBook(RefBook refBook) {
        N2oSimplePage page = page(N2oSimplePage.class);

        N2oTableWidget refBooksTable = page.widget(N2oTableWidget.class);

        refBooksTable.shouldExists();

        N2oDropdownButton createRefBook = refBooksTable.toolbar().topLeft()
                .button("Создать справочник", N2oDropdownButton.class);

        createRefBook.click();
        createRefBook.menuItem("Создать справочник").click();

        N2oFormWidget refBookCreateForm = page
                .widget(N2oFormWidget.class);
        Fields formFields = refBookCreateForm.fields();

        fillForm(formFields, refBook);
        page.toolbar().bottomRight().button("Сохранить").click();
    }

    private void fillForm(Fields formFields, RefBook refBook) {
        formFields.field("Код").control(N2oInputText.class).val(refBook.getCode());
        formFields.field("Наименование").control(N2oInputText.class).val(refBook.getName());
        formFields.field("Краткое наименование").control(N2oInputText.class).val(refBook.getShortName());
        formFields.field("Категория").control(N2oInputSelect.class).select(0);
        formFields.field("Описание").control(N2oTextArea.class).val(refBook.getDescription());
    }

    private void login() {
        LoginPage loginPage = open("/", LoginPage.class);
        loginPage.login(USERNAME, PASSWORD);
        logger.info("User logged in");
    }

    private StandardButtonWrapper getButton(Toolbar toolbar, String buttonName) {
        return new StandardButtonWrapper(toolbar.button(buttonName));
    }

    private void rdmPageShouldExists() {
        N2oPage rdmPage = page(N2oPage.class);
        rdmPage.shouldExists();
    }

    private void waitPublishing() {
        Selenide.sleep(SLEEP_TIME);
    }

    private TableWidgetWrapper getTableWidget() {
        return new TableWidgetWrapper(page(N2oSimplePage.class).widget(N2oTableWidget.class));
    }

    private static List<RefBookField> getRefBookFields(boolean withoutLink) {
        List<RefBookField> list = new ArrayList<>();
        FieldType[] fieldTypes = FieldType.values();
        for (FieldType fieldType : fieldTypes) {
            int ordinal = fieldType.ordinal() + 1;
            RefBookField refBookField = new RefBookField("code" + ordinal, "name" + ordinal, fieldType);

            if (fieldType.equals(FieldType.STRING)) {
                refBookField.setPrimaryKey(true);
            }

            if (withoutLink) {
                if (!refBookField.getAttributeTypeName().equals(FieldType.LINKED)) {
                    list.add(refBookField);
                }
            } else {
                list.add(refBookField);
            }
        }
        return list;
    }

    private N2oTabsRegion getTabsRegion() {
        return page(N2oStandardPage.class)
                .regions()
                .region(Condition.cssClass("n2o-tabs-region"), N2oTabsRegion.class);
    }

    private static RefBook getRefBook() {
        return new RefBook(
                "D" + RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                "shortName",
                "system",
                "description"
        );
    }
}
