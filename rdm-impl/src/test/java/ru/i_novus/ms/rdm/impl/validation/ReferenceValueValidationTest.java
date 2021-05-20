package ru.i_novus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceValueValidationTest {

    private final String ROW_FIELD_ID = "id";
    private final String ROW_FIELD_NAME = "name";
    private final String REFERENCE_VAL1 = "11";
    private final String REFERENCE_VAL2 = "22";
    private final String REF_ATTRIBUTE_CODE1 = "ref1";
    private final String REF_ATTRIBUTE_CODE2 = "ref2";
    private final String REF_BOOK_CODE = "REF_VALID";
    private final Integer VERSION_ID = 1;
    private final String REF_ATTRIBUTE_NAME1 = "Ссылка1";
    private final String REF_ATTRIBUTE_NAME2 = "Ссылка2";

    @Mock
    private VersionService versionService;

    @Mock
    private SearchDataService searchDataService;

    private Structure structure;

    private Row referenceRow;

    private Structure referredStructure;

    private Map<Structure.Reference, String> referenceKeyMap;

    @Before
    public void setUp() {
        Structure.Attribute id = Structure.Attribute.buildPrimary(ROW_FIELD_ID, "Идентификатор", FieldType.INTEGER, "");
        Structure.Attribute name = Structure.Attribute.build(ROW_FIELD_NAME, "Наименование", FieldType.STRING, "");
        Structure.Attribute ref1 = Structure.Attribute.build(REF_ATTRIBUTE_CODE1, REF_ATTRIBUTE_NAME1, FieldType.REFERENCE, "");
        Structure.Attribute ref2 = Structure.Attribute.build(REF_ATTRIBUTE_CODE2, REF_ATTRIBUTE_NAME2, FieldType.REFERENCE, "");
        Structure.Reference reference1 = new Structure.Reference(ref1.getCode(), REF_BOOK_CODE, toPlaceholder("name1"));
        Structure.Reference reference2 = new Structure.Reference(ref2.getCode(), REF_BOOK_CODE, toPlaceholder("name2"));
        structure = new Structure(asList(id, name, ref1, ref2), asList(reference1, reference2));

        referenceKeyMap = new HashMap<>(2);
        referenceKeyMap.put(reference1, REFERENCE_VAL1);
        referenceKeyMap.put(reference2, REFERENCE_VAL2);

        Map<String, Object> referenceRowMap = new HashMap<>(4);
        referenceRowMap.put(ROW_FIELD_ID, 1);
        referenceRowMap.put(ROW_FIELD_NAME, "name_1");
        referenceRowMap.put(REF_ATTRIBUTE_CODE1, new Reference(REFERENCE_VAL1, "display_" + REFERENCE_VAL1));
        referenceRowMap.put(REF_ATTRIBUTE_CODE2, new Reference(REFERENCE_VAL2, "display_" + REFERENCE_VAL2));
        referenceRow = new Row(referenceRowMap);

        referredStructure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, ""),
                        Structure.Attribute.build("name1", "Наименование1", FieldType.STRING, ""),
                        Structure.Attribute.build("name2", "Наименование2", FieldType.STRING, ""),
                        Structure.Attribute.build("id1", "id1", FieldType.INTEGER, ""),
                        Structure.Attribute.build("id2", "id2", FieldType.STRING, "")
                        ),
                null
        );
    }

    @Test
    public void testValidate() {

        RefBookVersionEntity referredEntity = new RefBookVersionEntity();
        referredEntity.setId(VERSION_ID);
        referredEntity.setStructure(referredStructure);
        referredEntity.setRefBook(new DefaultRefBookEntity());

        when(versionService.getLastPublishedVersion(eq(REF_BOOK_CODE)))
                .thenReturn(ModelGenerator.versionModel(referredEntity));

        ReferenceValueValidation referenceValueValidation = new ReferenceValueValidation(
                versionService, structure, referenceRow, singleton(REF_ATTRIBUTE_CODE2)
        );
        referenceValueValidation.appendRow(referenceRow);
        List<Message> messages = referenceValueValidation.validate();
        Assert.assertEquals(2, messages.size());
        Message expected1 = new Message(ReferenceValueValidation.REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE, REF_ATTRIBUTE_NAME1, REFERENCE_VAL1);
        Assert.assertTrue(messages.contains(expected1));
    }
}
