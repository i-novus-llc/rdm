package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.impl.entity.RefBookEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.impl.util.ModelGenerator;
import ru.inovus.ms.rdm.impl.validation.ReferenceValueValidation;

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

    private Structure structure;

    private Structure referenceStructure;

    private Map<Structure.Reference, String> referenceWithValueMap;

    @Before
    public void setUp() {
        Structure.Attribute id = Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, "");
        Structure.Attribute ref1 = Structure.Attribute.build(REF_ATTRIBUTE_CODE1, REF_ATTRIBUTE_NAME1, FieldType.REFERENCE, "");
        Structure.Attribute ref2 = Structure.Attribute.build(REF_ATTRIBUTE_CODE2, REF_ATTRIBUTE_NAME2, FieldType.REFERENCE, "");
        Structure.Attribute name = Structure.Attribute.build("name", "Наименование", FieldType.STRING, "");
        Structure.Reference reference1 = new Structure.Reference(ref1.getCode(), REF_BOOK_CODE, toPlaceholder("name1"));
        Structure.Reference reference2 = new Structure.Reference(ref2.getCode(), REF_BOOK_CODE, toPlaceholder("name2"));
        structure = new Structure(asList(id, ref1, ref2, name), asList(reference1, reference2));

        referenceWithValueMap = new HashMap<>();
        referenceWithValueMap.put(reference1, REFERENCE_VAL1);
        referenceWithValueMap.put(reference2, REFERENCE_VAL2);

        referenceStructure = new Structure(
                asList(
                        Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, ""),
                        Structure.Attribute.build("name1", "Наименование1", FieldType.STRING, ""),
                        Structure.Attribute.build("id1", "id1", FieldType.INTEGER, ""),
                        Structure.Attribute.build("id2", "id2", FieldType.STRING, ""),
                        Structure.Attribute.build("name2", "Наименование2", FieldType.STRING, "")
                        ),
                null
        );
    }

    @Test
    public void testValidate() {
        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(VERSION_ID);
        versionEntity.setStructure(referenceStructure);
        versionEntity.setRefBook(new RefBookEntity());

        when(versionService.getLastPublishedVersion(eq(REF_BOOK_CODE)))
                .thenReturn(ModelGenerator.versionModel(versionEntity));

        List<Message> messages = new ReferenceValueValidation(versionService, referenceWithValueMap, structure, singleton(REF_ATTRIBUTE_CODE2)).validate();
        Assert.assertEquals(1, messages.size());
        Message expected1 = new Message(ReferenceValueValidation.REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE, REF_ATTRIBUTE_NAME1, REFERENCE_VAL1);
        Assert.assertTrue(messages.contains(expected1));
    }
}
