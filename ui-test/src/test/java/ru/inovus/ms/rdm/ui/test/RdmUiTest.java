package ru.inovus.ms.rdm.ui.test;


import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import net.n2oapp.framework.autotest.api.component.control.Control;
import net.n2oapp.framework.autotest.api.component.control.InputText;
import net.n2oapp.framework.autotest.api.component.control.TextArea;
import net.n2oapp.framework.autotest.impl.component.control.N2oCheckbox;
import net.n2oapp.framework.autotest.impl.component.control.N2oDateInput;
import net.n2oapp.framework.autotest.impl.component.control.N2oInputSelect;
import net.n2oapp.framework.autotest.impl.component.control.N2oSelect;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.SystemProperties;
import ru.inovus.ms.rdm.ui.test.page.LoginPage;
import ru.inovus.ms.rdm.ui.test.model.FieldType;
import ru.inovus.ms.rdm.ui.test.model.RefBook;
import ru.inovus.ms.rdm.ui.test.model.RefBookField;
import ru.inovus.ms.rdm.ui.test.page.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.n2oapp.framework.autotest.N2oSelenide.open;

public class RdmUiTest {

    private static final Logger logger = LoggerFactory.getLogger(RdmUiTest.class);

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private static final long SLEEP_TIME = TimeUnit.SECONDS.toMillis(6);
    private static final int REF_BOOK_DATA_ROWS_CREATE_COUNT = 3;

    //легче работать с упорядочным по id списком, поэтому через этот класс задаем порядок
    private final AtomicInteger refBookDataIdSeq = new AtomicInteger();

    @BeforeClass
    public static void setUp() {
        Configuration.baseUrl = getBaseUrl();
        Configuration.timeout = 8000;
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

    /**
     * Создает и публикует два справочника, один из которых ссылочный с ссылкой на первый справочник
     */
    @Test
    public void testCreateRefBookAndRefBookWithReference() {
        RefBook simpleRefBook = generateRefBook(REF_BOOK_DATA_ROWS_CREATE_COUNT, null, FieldType.INTEGER, FieldType.STRING, FieldType.DOUBLE, FieldType.BOOLEAN, FieldType.DATE);
        RefBook refBookWithReference = generateRefBook(REF_BOOK_DATA_ROWS_CREATE_COUNT, simpleRefBook, FieldType.INTEGER, FieldType.STRING, FieldType.DOUBLE, FieldType.BOOLEAN, FieldType.REFERENCE, FieldType.DATE);

        RefBookListPage refBookListPage = login();
        refBookListPage.shouldExists();
        createRefBook(refBookListPage, simpleRefBook, simpleRefBook.getRows());
        refBookListPage.shouldExists();
        search(refBookListPage, simpleRefBook);
        refBookListPage.rowShouldHaveTexts(0, Collections.singletonList(simpleRefBook.getCode()));
        RefBookEditPage refBookEditPage = refBookListPage.openRefBookEditPage(0);
        editRefBook(refBookEditPage, simpleRefBook.getRows());

        createRefBook(refBookListPage, refBookWithReference, refBookWithReference.getRows());

        search(refBookListPage, refBookWithReference);
        refBookListPage.rowShouldHaveTexts(0, Collections.singletonList(refBookWithReference.getCode()));
        refBookListPage.deleteRow(0);
        search(refBookListPage, refBookWithReference);
        refBookListPage.rowShouldHaveSize(0);

        search(refBookListPage, simpleRefBook);
        refBookListPage.rowShouldHaveTexts(0, Collections.singletonList(simpleRefBook.getCode()));
        refBookListPage.deleteRow(0);
        search(refBookListPage, simpleRefBook);
        refBookListPage.rowShouldHaveSize(0);
    }

    private void search(RefBookListPage refBookListPage,  RefBook refBook) {
        fillInputText(refBookListPage.codeFilter(), refBook.getCode());
        fillInputText(refBookListPage.nameFilter(), refBook.getName());
        refBookListPage.search();
    }

    private void createRefBook(RefBookListPage refBookListPage, RefBook refBook, List<Map<RefBookField, Object>> refBookRows) {
        RefBookCreateFormWidget refBookCreateFormWidget = refBookListPage.openCreateFormPage();
        refBookCreateFormWidget.shouldExists();
        fillRefBookForm(refBookCreateFormWidget, refBook);
        RefBookEditPage refBookEditPage = refBookCreateFormWidget.save();
        refBookEditPage.shouldExists();
        StructureListWidget structureListWidget = refBookEditPage.structure();
        structureListWidget.shouldExists();

        Set<RefBookField> fieldsToFirstRefBook = refBookRows.get(0).keySet();
        createStructure(structureListWidget, fieldsToFirstRefBook);

        DataListWidget dataListWidget = refBookEditPage.data();
        dataListWidget.shouldExists();

        List<String> addedRowsNameColumnValues = new ArrayList<>();
        for (Map<RefBookField, Object> row : refBookRows) {
            DataFormModal dataForm = dataListWidget.addRowForm();
            dataForm.shouldExists();
            fillDataForm(dataForm, row);
            dataForm.save();
            addedRowsNameColumnValues.add((String) row.entrySet().stream().filter(entry -> "name".equals(entry.getKey().getCode())).findAny().get().getValue());
            dataListWidget.rowShouldHaveTexts(1, addedRowsNameColumnValues);
        }
        refBookEditPage.publish();
        waitPublishing();
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

    private void editRefBook(RefBookEditPage refBookEditPage, List<Map<RefBookField, Object>> existedRows) {
        List<String> addedRowsNameColumnValues = existedRows.stream().map(row -> (String) row.entrySet().stream()
                .filter(entry -> "name".equals(entry.getKey().getCode()))
                .findAny().get().getValue()).collect(Collectors.toList());
        refBookEditPage.shouldExists();
        DataListWidget dataListWidget = refBookEditPage.data();
        DataFormModal addForm = dataListWidget.addRowForm();
        List<Map<RefBookField, Object>> refBookRows = generateRefBookRows(1, null, FieldType.INTEGER, FieldType.STRING, FieldType.DOUBLE, FieldType.DATE, FieldType.BOOLEAN);
        Map<RefBookField, Object> row = refBookRows.get(0);
        fillDataForm(addForm, row);
        addForm.save();
        addedRowsNameColumnValues.add((String) row.entrySet().stream()
                .filter(entry -> "name".equals(entry.getKey().getCode()))
                .findAny().get().getValue());
        dataListWidget.rowShouldHaveTexts(1, addedRowsNameColumnValues);

        DataFormModal editForm = dataListWidget.editRowForm(addedRowsNameColumnValues.size()-1);
        String newNameVal = "Другое наименование";
        fillInputText(editForm.stringInput("Наименование"), newNameVal);
        editForm.edit();
        addedRowsNameColumnValues.set(addedRowsNameColumnValues.size()-1, newNameVal);
        dataListWidget.rowShouldHaveTexts(1, addedRowsNameColumnValues);

        dataListWidget.deleteRowForm(addedRowsNameColumnValues.size()-1);
        addedRowsNameColumnValues.remove(addedRowsNameColumnValues.size()-1);
        dataListWidget.rowShouldHaveTexts(1, addedRowsNameColumnValues);
        refBookEditPage.publish();

    }

    private void fillDataForm(DataFormModal dataForm, Map<RefBookField, Object> row) {
        row.forEach((key, value) -> {
            switch (key.getAttributeTypeName()) {
                case STRING -> fillInputText(dataForm.stringInput(key.getName()), value.toString());
                case INTEGER -> fillInputText(dataForm.integerInput(key.getName()), value.toString());
                case DOUBLE -> fillInputText(dataForm.doubleInput(key.getName()), value.toString());
                case DATE -> fillInputText(dataForm.dateInput(key.getName()), (value.toString()));
                case BOOLEAN -> {
                    N2oCheckbox checkbox = dataForm.booleanInput(key.getName());
                    checkbox.shouldExists();
                    checkbox.setChecked(true);
                    checkbox.shouldBeChecked();
                }
                case REFERENCE -> {
                    N2oInputSelect referenceInput = dataForm.referenceInput(key.getName());
                    referenceInput.expandPopUpOptions();
                    referenceInput.select(((int) value));
                }
                default -> throw new IllegalArgumentException(key.getAttributeTypeName() + " invalid field type");
            }
        });

    }

    private void fillStructureForm(StructureFormModal structureForm, RefBookField refBookField) {
        fillInputText(structureForm.codeInput(), refBookField.getCode());
        fillInputText(structureForm.nameInput(), refBookField.getName());
        N2oSelect typeInput = structureForm.typeInput();
        typeInput.select(Condition.text(refBookField.getAttributeTypeName().getTranslated()));
        typeInput.shouldHaveValue(refBookField.getAttributeTypeName().getTranslated());
        if (refBookField.getAttributeTypeName().equals(FieldType.REFERENCE)) {
            N2oInputSelect refBookInput = structureForm.refBookInput();
            refBookInput.shouldBeEmpty();
            refBookInput.val(refBookField.getReference().getCode());
            refBookInput.expandPopUpOptions();
            refBookInput.select(Condition.text(refBookField.getReference().getCode()));
            refBookInput.shouldSelected(refBookField.getReference().getCode());

            N2oInputSelect displayAttrInput = structureForm.displayAttrInput();
            displayAttrInput.select(0);
            displayAttrInput.shouldHaveValue("Идентификатор");
        }
        if (refBookField.isPrimaryKey()) {
            N2oCheckbox pkInput = structureForm.pkInput();
            pkInput.setChecked(true);
            pkInput.shouldBeChecked();
        }
    }



    private void fillRefBookForm(RefBookCreateFormWidget refBookCreateFormWidget, RefBook refBook) {
        fillInputText(refBookCreateFormWidget.codeInput(), refBook.getCode());
        fillInputText(refBookCreateFormWidget.nameInput(), refBook.getName());
        fillInputText(refBookCreateFormWidget.shortNameInput(), refBook.getShortName());
        fillInputText(refBookCreateFormWidget.descriptionInput(), refBook.getDescription());

    }

    private void fillInputText(Control inputText, String value) {
        inputText.shouldExists();
        if(inputText instanceof InputText) {
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

    private RefBookListPage login() {
        LoginPage loginPage = open("/", LoginPage.class);
        RefBookListPage refBookListPage = loginPage.login(USERNAME, PASSWORD);
        logger.info("User logged in");
        return refBookListPage;
    }

    private void waitPublishing() {
        Selenide.sleep(SLEEP_TIME);
    }
    private List<Map<RefBookField, Object>> generateRefBookRows(int rowsCount, RefBook reference, FieldType ... types) {
        List<Map<RefBookField, Object>> result = new ArrayList<>(rowsCount);
        for(int i = 0; i < rowsCount; i++) {
            Map<RefBookField, Object> row = new LinkedHashMap<>(types.length);
            for (FieldType fieldType : types) {
                switch (fieldType) {
                    case STRING -> row.put(new RefBookField("name", "Наименование", fieldType, false, null), RandomStringUtils.randomAlphabetic(5));
                    case INTEGER -> row.put(new RefBookField("id", "Идентификатор", fieldType, true, null), String.valueOf(refBookDataIdSeq.getAndIncrement()));
                    case DOUBLE -> row.put(new RefBookField("some_double", "Некоторое дробное поле", fieldType, false, null), String.valueOf(RandomUtils.nextDouble()).substring(0, 3));
                    case DATE -> row.put(new RefBookField("some_date", "Некоторая дата", fieldType, false, null), LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    case BOOLEAN -> row.put(new RefBookField("some_boolean", "Некоторое булеан поле", fieldType, false, null), RandomUtils.nextBoolean());
                    case REFERENCE -> row.put(new RefBookField("some_reference", "Некоторое ссылочное поле", fieldType, false, reference), 0);
                    default -> throw new IllegalArgumentException(fieldType + " invalid field type");
                }
            }
            result.add(row);
        }
        return result;
    }

    private RefBook generateRefBook(int rowsCount, RefBook reference, FieldType ... types) {
        return new RefBook(
                "D" + RandomStringUtils.randomAlphabetic(5),
                RandomStringUtils.randomAlphabetic(5),
                "shortName",
                "system",
                "description",
                generateRefBookRows(rowsCount, reference, types));
    }
}
