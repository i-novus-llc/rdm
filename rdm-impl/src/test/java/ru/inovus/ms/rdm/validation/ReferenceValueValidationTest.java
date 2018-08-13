package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceValueValidationTest {

    private final String REFERENCE_VAL1 = "11";
    private final String REFERENCE_VAL2 = "22";
    private final String REF_ATTRIBUTE_CODE1 = "ref1";
    private final String REF_ATTRIBUTE_CODE2 = "ref2";
    private final Integer VERSION_ID = 1;
    private final String REF_ATTRIBUTE_NAME1 = "Ссылка1";
    private final String REF_ATTRIBUTE_NAME2 = "Ссылка2";


    @Mock
    private VersionService versionService;

    private Structure structure;

    private Structure referenceStructure;

    private Map<Structure.Reference, String> referenceWithValueMap;

    @Before
    public void setUp() throws Exception {
        Structure.Attribute id = Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, "");
        Structure.Attribute ref1 = Structure.Attribute.build(REF_ATTRIBUTE_CODE1, REF_ATTRIBUTE_NAME1, FieldType.REFERENCE, false, "");
        Structure.Attribute ref2 = Structure.Attribute.build(REF_ATTRIBUTE_CODE2, REF_ATTRIBUTE_NAME2, FieldType.REFERENCE, false, "");
        Structure.Attribute name = Structure.Attribute.build("name", "Наименование", FieldType.STRING, false, "");
        Structure.Reference reference1 = new Structure.Reference(ref1.getCode(), VERSION_ID, "id1", Collections.singletonList("name1"), null);
        Structure.Reference reference2 =new Structure.Reference(ref2.getCode(), VERSION_ID, "id2", Collections.singletonList("name2"), null);
        structure = new Structure(Arrays.asList(id, ref1, ref2, name), Arrays.asList(reference1, reference2));
        referenceWithValueMap = new HashMap<>();
        referenceWithValueMap.put(reference1, REFERENCE_VAL1);
        referenceWithValueMap.put(reference2, REFERENCE_VAL2);

        referenceStructure = new Structure(
                Arrays.asList(
                        Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, ""),
                        Structure.Attribute.build("name1", "Наименование1", FieldType.STRING, false, ""),
                        Structure.Attribute.build("id1", "id1", FieldType.INTEGER, false, ""),
                        Structure.Attribute.build("id2", "id2", FieldType.STRING, false, ""),
                        Structure.Attribute.build("name2", "Наименование2", FieldType.STRING, false, "")
                        ),
                null
        );

    }

    @Test
    public void testValidate() throws Exception {
        when(versionService.getStructure(eq(VERSION_ID))).thenReturn(referenceStructure);
        when(versionService.search(eq(VERSION_ID), any(SearchDataCriteria.class))).thenReturn(new PageImpl<RowValue>(Collections.<RowValue>emptyList()));
        List<Message> messages = new ReferenceValueValidation(versionService, referenceWithValueMap, structure, Collections.singletonList(REF_ATTRIBUTE_CODE2)).validate();
        Assert.assertTrue(messages.size() == 1);
        Message expected1 = new Message(ReferenceValueValidation.ERROR_CODE, REF_ATTRIBUTE_NAME1, REFERENCE_VAL1);
        Assert.assertTrue(messages.contains(expected1));
    }
}
