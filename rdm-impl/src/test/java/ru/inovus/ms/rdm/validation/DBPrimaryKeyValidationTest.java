package ru.inovus.ms.rdm.validation;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.platform.i18n.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DBPrimaryKeyValidationTest {

    private final String STORAGE_CODE = "test_storage";
    private final String PK_STRING = "pks";
    private final String PK_REFERENCE = "pkr";
    private final String PK_FLOAT = "pkf";
    private final String PK_DATE = "pkd";
    private final String PK_BOOL = "pkb";
    private final String PK_INTEGER = "pki";
    private final String NOT_PK = "npk";

    @Mock
    private SearchDataService searchDataService;

    private Structure pkStructure;

    private Structure noPkStructure;

    private Row pkRow;

    private Row noPkRow;

    @Before
    public void setUp() throws Exception {
        pkStructure = new Structure();
        pkStructure.setAttributes(Arrays.asList(
                Structure.Attribute.buildPrimary(PK_STRING, "string", FieldType.STRING, "string"),
                Structure.Attribute.buildPrimary(PK_REFERENCE, "reference", FieldType.REFERENCE, "count"),
                Structure.Attribute.buildPrimary(PK_FLOAT, "float", FieldType.FLOAT, "float"),
                Structure.Attribute.buildPrimary(PK_DATE, "date", FieldType.DATE, "date"),
                Structure.Attribute.buildPrimary(PK_BOOL, "boolean", FieldType.BOOLEAN, "boolean"),
                Structure.Attribute.buildPrimary(PK_INTEGER, "integer", FieldType.INTEGER, "integer"),
                Structure.Attribute.build(NOT_PK, "not_pk", FieldType.INTEGER, "integer")
        ));

        Map<String, Object> pkRowMap = new HashMap<>();
        pkRowMap.put(PK_STRING, "test Value");
        pkRowMap.put(PK_REFERENCE, new Reference());
        pkRowMap.put(PK_FLOAT, "test Value");
        pkRowMap.put(PK_DATE, "test Value");
        pkRowMap.put(PK_BOOL, "test Value");
        pkRowMap.put(PK_INTEGER, "test Value");
        pkRowMap.put(NOT_PK, "test Value");
        pkRow = new Row(pkRowMap);

        noPkStructure = new Structure();
        noPkStructure.setAttributes(Collections.singletonList(
                Structure.Attribute.build(NOT_PK, "not_pk", FieldType.INTEGER, "integer")
        ));

        Map<String, Object> noPkRowMap = new HashMap<>();
        noPkRowMap.put(NOT_PK, "test Value");
        noPkRow = new Row(noPkRowMap);
    }


    @Test
    public void testValidateWithoutCreateCriteria() throws Exception {

        when(searchDataService.getPagedData(anyObject())).thenReturn(new CollectionPage<>(1, Collections.singletonList(new LongRowValue(1L, emptyList())), new Criteria()));

        ErrorAttributeHolderValidation validation = new DBPrimaryKeyValidation(searchDataService, pkStructure, pkRow, STORAGE_CODE);
        List<Message> messages = validation.validate();
        Set<String> errorAttributes = validation.getErrorAttributes();
        Assert.assertEquals(1, messages.size());
        Assert.assertEquals(6, errorAttributes.size());

        validation = new DBPrimaryKeyValidation(searchDataService, pkStructure, pkRow, STORAGE_CODE);
        validation.setErrorAttributes(Collections.singleton(PK_STRING));
        messages = validation.validate();
        errorAttributes = validation.getErrorAttributes();
        Assert.assertEquals(0, messages.size());
        Assert.assertEquals(1, errorAttributes.size());

        validation = new DBPrimaryKeyValidation(searchDataService, noPkStructure, noPkRow, STORAGE_CODE);
        validation.setErrorAttributes(Collections.singleton(PK_STRING));
        messages = validation.validate();
        errorAttributes = validation.getErrorAttributes();
        Assert.assertEquals(0, messages.size());
        Assert.assertEquals(1, errorAttributes.size());

        when(searchDataService.getPagedData(anyObject())).thenReturn(new CollectionPage<>(0, emptyList(), new Criteria()));

        validation = new DBPrimaryKeyValidation(searchDataService, pkStructure, pkRow, STORAGE_CODE);
        messages = validation.validate();
        errorAttributes = validation.getErrorAttributes();
        Assert.assertEquals(0, messages.size());
        Assert.assertEquals(0, errorAttributes.size());

        validation = new DBPrimaryKeyValidation(searchDataService, pkStructure, pkRow, STORAGE_CODE);
        validation.setErrorAttributes(Collections.singleton(PK_STRING));
        messages = validation.validate();
        errorAttributes = validation.getErrorAttributes();
        Assert.assertEquals(0, messages.size());
        Assert.assertEquals(1, errorAttributes.size());
    }
}
