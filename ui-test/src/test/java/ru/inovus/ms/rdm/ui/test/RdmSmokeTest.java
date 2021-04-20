package ru.inovus.ms.rdm.ui.test;


import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import net.n2oapp.framework.autotest.N2oSelenide;
import net.n2oapp.framework.autotest.api.collection.Fields;
import net.n2oapp.framework.autotest.api.component.button.StandardButton;
import net.n2oapp.framework.autotest.api.component.field.StandardField;
import net.n2oapp.framework.autotest.api.component.region.TabsRegion;
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
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.ui.test.custom.CustomModalFormWidget;
import ru.inovus.ms.rdm.ui.test.login.FieldType;
import ru.inovus.ms.rdm.ui.test.login.LoginPage;
import ru.inovus.ms.rdm.ui.test.model.RefBookCreateModel;
import ru.inovus.ms.rdm.ui.test.model.RefBookField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class RdmSmokeTest {

    private static final Logger logger = LoggerFactory.getLogger(RdmSmokeTest.class);

    private static final long LOADING_TIME = TimeUnit.MINUTES.toMillis(1);
    private static final String RDM_BASE_URL = "http://localhost:8080";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    @BeforeAll
    public static void setUp() {
        Configuration.baseUrl = RDM_BASE_URL;
    }

    @AfterAll
    public static void tearDown() {
        Selenide.closeWebDriver();
    }

    @Test
    void testRdmPage() {
        login();

        N2oPage page = N2oSelenide.page(N2oPage.class);

        page.shouldExists();

        // ------------------ Создание справочника ------------------------ //
        N2oSimplePage refBookCreatePage = createRefBook(page);

        // ------------------ Редактирование справочника ------------------------ //
        N2oStandardPage refBookEditPage = N2oSelenide.page(N2oStandardPage.class);
        N2oTabsRegion tabsRegion = refBookEditPage.regions().region(Condition.cssClass("n2o-tabs-region"), N2oTabsRegion.class);

        // ------------------ Добавление полей ------------------------ //
        tabsRegion.element().waitUntil(Condition.visible, LOADING_TIME);
        TabsRegion.TabItem structureTab = tabsRegion
                .tab(Condition.text("Структура"));
        structureTab.shouldExists();
        structureTab.click();
        createFields(refBookCreatePage, structureTab);

        CustomModalFormWidget modalForm = refBookCreatePage.widget(CustomModalFormWidget.class);

        modalForm.waitUntil(Condition.not(Condition.visible), LOADING_TIME);
        // ------------------ Добавление данных ------------------------ //
        TabsRegion.TabItem dataTab = tabsRegion.tab(Condition.text("Данные"));
        dataTab.shouldExists();
        dataTab.click();

        createRefBookDataRows(refBookCreatePage, dataTab);

        modalForm.waitUntil(Condition.not(Condition.visible), LOADING_TIME);

        N2oSelenide.open("/", N2oPage.class);

        logger.info("Test rdm page success");
    }

    private void createRefBookDataRows(N2oSimplePage refBookCreatePage, TabsRegion.TabItem dataTab) {
        dataTab.shouldBeActive();
        N2oTableWidget refBookDataEditTable = dataTab.content().widget(N2oTableWidget.class);

        CustomModalFormWidget modalForm = refBookCreatePage.widget(CustomModalFormWidget.class);

        for (int i = 0; i < 3; i++) {
            StandardButton standardButton = refBookDataEditTable.toolbar().topRight().button("Добавить");
            modalForm.waitUntil(Condition.not(Condition.visible), LOADING_TIME);
            standardButton.click();
            for (RefBookField refBookField : getRefBookFields()) {
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
                }
            }
            modalForm.save();
        }
    }

    private void createFields(N2oSimplePage refBookCreatePage, TabsRegion.TabItem tabItem) {
        List<RefBookField> refBookFields = getRefBookFields();
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
            structureModalFormFields.field("Тип").control(N2oSelect.class)
                    .select(Condition.text(refBookField.getAttributeTypeName().getTranslated()));
            modalForm.save();

            logger.info("Add ref book field with name: \"{}\", type: \"{}\" success", refBookField.getName(),
                    refBookField.getAttributeTypeName().getTranslated());
        }
    }

    private List<RefBookField> getRefBookFields() {
        List<RefBookField> list = new ArrayList<>();
        FieldType[] fieldTypes = FieldType.values();
        for (FieldType fieldType : fieldTypes) {
            int ordinal = fieldType.ordinal() + 1;
            list.add(new RefBookField("code" + ordinal, "name" + ordinal, fieldType));
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
        RefBookCreateModel refBookCreateModel = getRefBookCreateModel();
        formFields.field("Код").control(N2oInputText.class).val(refBookCreateModel.getCode());
        formFields.field("Наименование").control(N2oInputText.class).val(refBookCreateModel.getName());
        formFields.field("Краткое наименование").control(N2oInputText.class).val(refBookCreateModel.getShortName());
        //Локально не создается элемент для выпадающего элемента почему то
//        formFields.field("Категория").control(N2oInputSelect.class).select(0);
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
