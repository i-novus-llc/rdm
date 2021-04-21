package ru.inovus.ms.rdm.ui.test;


import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.collection.Fields;
import net.n2oapp.framework.autotest.api.collection.Toolbar;
import net.n2oapp.framework.autotest.api.component.button.StandardButton;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.ui.test.custom.component.page.LoginPage;
import ru.inovus.ms.rdm.ui.test.custom.component.widget.CustomModalFormWidget;
import ru.inovus.ms.rdm.ui.test.custom.wrapper.N2oTableWidgetWrapper;
import ru.inovus.ms.rdm.ui.test.model.FieldType;
import ru.inovus.ms.rdm.ui.test.model.RefBookCreateModel;
import ru.inovus.ms.rdm.ui.test.model.RefBookField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class RdmSmokeTest {

    private static final Logger logger = LoggerFactory.getLogger(RdmSmokeTest.class);

    private static final String RDM_BASE_URL = "http://localhost:8080";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private static final long LOADING_TIME = TimeUnit.MINUTES.toMillis(1);
    private static final int REF_BOOK_DATA_ROWS_CREATE_COUNT = 3;

    private RefBookCreateModel refBookCreateModel;
    private List<RefBookField> refBookFields;
    private List<RefBookField> refBookFieldsWithoutLink;

    @BeforeAll
    public static void setUp() {
        Configuration.baseUrl = RDM_BASE_URL;
    }

    @AfterAll
    public static void tearDown() {
        Selenide.closeWebDriver();
    }

    @BeforeEach
    public void init() {
        refBookCreateModel = getRefBookCreateModel();
        refBookFields = getRefBookFields(true);
        refBookFieldsWithoutLink = getRefBookFields(false);
    }

    @Test
    void testRdmPage() {
        login();

        N2oPage page = N2oSelenide.page(N2oPage.class);

        page.shouldExists();

        // ------------------ Создание справочника ------------------------ //
        N2oSimplePage refBookOperationsPage = createRefBook(page);

        // ------------------ Редактирование справочника ------------------------ //
        N2oStandardPage refBookEditPage = N2oSelenide.page(N2oStandardPage.class);
        N2oTabsRegion tabsRegion = refBookEditPage.regions().region(Condition.cssClass("n2o-tabs-region"), N2oTabsRegion.class);

        // ------------------ Добавление полей ------------------------ //
        tabsRegion.element().waitUntil(Condition.visible, LOADING_TIME);

        createRefBookFields(refBookOperationsPage, tabsRegion, refBookFields);

        CustomModalFormWidget modalForm = refBookOperationsPage.widget(CustomModalFormWidget.class);

        modalForm.waitUntil(Condition.not(Condition.visible), LOADING_TIME);

        // ------------------ Добавление данных ------------------------ //
        createRefBookDataRows(refBookOperationsPage, tabsRegion);

        modalForm.waitUntil(Condition.not(Condition.visible), LOADING_TIME);

        // ------------------ Публикация справочника ------------------------ //
        publishRefBook(refBookOperationsPage);

        // ------------------ Редактирование справочника ------------------------ //
        N2oTableWidget refBookEditTableWidget = refBookOperationsPage.widget(N2oTableWidget.class);

        TableWidget.Filters filters = refBookEditTableWidget.filters();
        N2oTableWidgetWrapper n2oTableWidgetWrapper = new N2oTableWidgetWrapper(refBookEditTableWidget);
        n2oTableWidgetWrapper.waitUntilFilterVisible(Condition.visible, TimeUnit.MINUTES.toMillis(1));
        filters.shouldBeVisible();

        Fields searchFilterFields = filters.fields();
        searchFilterFields.field("Название справочника").control(N2oInputText.class).val(refBookCreateModel.getName());
        searchFilterFields.field("Код справочника").control(N2oInputText.class).val(refBookCreateModel.getCode());

        //ждем пока справочник опубликуется
        afterLagging();

        filters.search();

        refBookEditTableWidget.columns().rows().row(0).click();
        refBookEditTableWidget.toolbar().topLeft().button("Изменить справочник").click();

        editRefBookDataRows(refBookOperationsPage, tabsRegion);

        /*
        сложно определить в какой момент можно перейти к опубликованию справочника
        может возникнуть ситуация, что строка из данных не удалится перед опубликованием
         */
        afterLagging();

        // ------------------ Публикация справочника ------------------------ //
        publishRefBook(refBookOperationsPage);

        refBookOperationsPage = createRefBook(page);

        logger.info("Test rdm page success");
    }

    private void afterLagging() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(45));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void editRefBookDataRows(N2oSimplePage refBookOperationsPage, N2oTabsRegion tabsRegion) {
        TabsRegion.TabItem tabItem = tabsRegion.tab(Condition.text("Данные"));

        tabItem.click();

        N2oTableWidget refBookDataEditTable = tabItem.content().widget(N2oTableWidget.class);
        refBookDataEditTable.shouldExists();

        Toolbar toolbar = refBookDataEditTable.toolbar().topRight();

        CustomModalFormWidget customModalFormWidget = refBookOperationsPage.widget(CustomModalFormWidget.class);
        createRefBookDataRow(refBookDataEditTable, customModalFormWidget);

        customModalFormWidget.waitUntil(Condition.not(Condition.visible), LOADING_TIME);

        TableWidget.Rows rows = refBookDataEditTable.columns().rows();
        rows.row(2).click();
        toolbar.button("Изменить").click();

        fillFields(customModalFormWidget);
        customModalFormWidget.save("Изменить");

        customModalFormWidget.waitUntil(Condition.not(Condition.visible), LOADING_TIME);

        rows.row(3).click();
        toolbar.button("Удалить").click();

        Page.Dialog deleteDialog = refBookOperationsPage.dialog("Удалить");
        deleteDialog.shouldBeVisible();
        deleteDialog.click("Да");
    }

    private void publishRefBook(N2oSimplePage n2oSimplePage) {
        N2oDropdownButton n2oDropdownButton = n2oSimplePage.widget(N2oFormWidget.class)
                .toolbar().bottomLeft()
                .button("Действия", N2oDropdownButton.class);
        n2oDropdownButton.click();
        n2oDropdownButton
                .menuItem("Опубликовать").click();

        n2oSimplePage.dialog("Публикация справочника").shouldBeVisible();
        n2oSimplePage.dialog("Публикация справочника").click("Опубликовать");
    }

    private void createRefBookDataRows(N2oSimplePage refBookCreatePage, N2oTabsRegion tabsRegion) {
        TabsRegion.TabItem dataTab = tabsRegion.tab(Condition.text("Данные"));
        dataTab.shouldExists();
        dataTab.click();
        dataTab.shouldBeActive();
        N2oTableWidget refBookDataEditTable = dataTab.content().widget(N2oTableWidget.class);

        CustomModalFormWidget modalForm = refBookCreatePage.widget(CustomModalFormWidget.class);

        for (int i = 0; i < REF_BOOK_DATA_ROWS_CREATE_COUNT; i++) {
            createRefBookDataRow(refBookDataEditTable, modalForm);
        }
    }

    private void createRefBookDataRow(N2oTableWidget refBookDataEditTable, CustomModalFormWidget modalForm) {
        StandardButton standardButton = refBookDataEditTable.toolbar().topRight().button("Добавить");
        modalForm.waitUntil(Condition.not(Condition.visible), LOADING_TIME);
        standardButton.click();
        fillFields(modalForm);
        modalForm.save("Сохранить");
    }

    private void fillFields(CustomModalFormWidget modalForm) {
        for (RefBookField refBookField : refBookFields) {
            Fields refBookDataFields = modalForm.fields();
            StandardField field = refBookDataFields.field(refBookField.getName());
            switch (refBookField.getAttributeTypeName()) {
                case STRING -> field.control(N2oInputText.class).val(RandomStringUtils.randomAlphabetic(5));
                case INTEGER -> field.control(N2oInputText.class).val(String.valueOf(RandomUtils.nextInt()));
                case DOUBLE -> field.control(N2oInputText.class).val(String.valueOf(RandomUtils.nextDouble()));
                case DATE -> field.control(N2oDateInput.class).val(LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                case BOOLEAN -> field.control(N2oCheckbox.class).setChecked(RandomUtils.nextBoolean());
            }
        }
    }

    private void createRefBookFields(N2oSimplePage refBookCreatePage, N2oTabsRegion tabsRegion, List<RefBookField> refBookFields) {
        TabsRegion.TabItem tabItem = tabsRegion
                .tab(Condition.text("Структура"));
        tabItem.shouldExists();
        tabItem.click();
        for (RefBookField refBookField : refBookFields) {
            N2oTableWidget refBookStructureEditTable = tabItem.content().widget(N2oTableWidget.class);

            CustomModalFormWidget modalForm = refBookCreatePage.widget(CustomModalFormWidget.class);

            StandardButton standardButton = refBookStructureEditTable.toolbar()
                    .bottomRight()
                    .button("Добавить");

            modalForm.waitUntil(Condition.not(Condition.visible), LOADING_TIME);

            standardButton.click();

            Fields structureModalFormFields = modalForm.fields();
            structureModalFormFields.field("Код").control(N2oInputText.class).val(refBookField.getCode());
            structureModalFormFields.field("Наименование").control(N2oInputText.class).val(refBookField.getName());
            if (refBookField.isPrimaryKey()) {
                structureModalFormFields.field("Первичный ключ").control(N2oCheckbox.class).setChecked(true);
            }

            if (refBookField.getAttributeTypeName() == null) {
                structureModalFormFields.field("Тип").control(N2oSelect.class)
                        .select(Condition.text("Ссылочный"));
                N2oInputSelect selectRefBook = structureModalFormFields.field("Выбор справочника").control(N2oInputSelect.class);
                selectRefBook.expandPopUpOptions();
                selectRefBook.select(Condition.matchesText(refBookField.getCode()));
            } else {
                structureModalFormFields.field("Тип").control(N2oSelect.class)
                        .select(Condition.text(refBookField.getAttributeTypeName().getTranslated()));
            }
            modalForm.save("Сохранить");

            logger.info("Add ref book field with name: \"{}\", type: \"{}\" success", refBookField.getName(),
                    refBookField.getAttributeTypeName().getTranslated());
        }
    }

    private List<RefBookField> getRefBookFields(boolean withoutLink) {
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

    private N2oSimplePage createRefBook(N2oPage page) {
        N2oSimplePage refBookCreatePage = N2oSelenide.page(N2oSimplePage.class);
        N2oTableWidget refBookTable = refBookCreatePage
                .widget(N2oTableWidget.class);

        refBookTable.shouldExists();
        N2oDropdownButton createRefBook = refBookTable.toolbar().topLeft()
                .button("Создать справочник", N2oDropdownButton.class);

        createRefBook.click();
        createRefBook.menuItem("Создать справочник").click();

        N2oFormWidget refBookCreateForm = refBookCreatePage
                .widget(N2oFormWidget.class);
        Fields formFields = refBookCreateForm.fields();

        fillForm(formFields);
        page.toolbar().bottomRight().button("Сохранить").click();

        return refBookCreatePage;
    }

    private void fillForm(Fields formFields) {
        formFields.field("Код").control(N2oInputText.class).val(refBookCreateModel.getCode());
        formFields.field("Наименование").control(N2oInputText.class).val(refBookCreateModel.getName());
        formFields.field("Краткое наименование").control(N2oInputText.class).val(refBookCreateModel.getShortName());
        formFields.field("Категория").control(N2oInputSelect.class).select(0);
        formFields.field("Описание").control(N2oTextArea.class).val(refBookCreateModel.getDescription());
    }

    private RefBookCreateModel getRefBookCreateModel() {
        return new RefBookCreateModel(
                RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                "shortName",
                "system",
                "description"
        );
    }

    private void login() {
        LoginPage loginPage = N2oSelenide.open("/", LoginPage.class);
        loginPage.login(USERNAME, PASSWORD);
        logger.info("User logged in");
    }
}
