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
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
    @Mock
    private RefBookVersionRepository versionRepository;

    private Structure structure;

    private Structure referenceStructure;

    private Map<Structure.Reference, String> referenceWithValueMap;

    @Before
    public void setUp() throws Exception {
        Structure.Attribute id = Structure.Attribute.buildPrimary("id", "Идентификатор", FieldType.INTEGER, "");
        Structure.Attribute ref1 = Structure.Attribute.build(REF_ATTRIBUTE_CODE1, REF_ATTRIBUTE_NAME1, FieldType.REFERENCE, "");
        Structure.Attribute ref2 = Structure.Attribute.build(REF_ATTRIBUTE_CODE2, REF_ATTRIBUTE_NAME2, FieldType.REFERENCE, "");
        Structure.Attribute name = Structure.Attribute.build("name", "Наименование", FieldType.STRING, "");
        Structure.Reference reference1 = new Structure.Reference(ref1.getCode(), REF_BOOK_CODE, toPlaceholder("name1"));
        Structure.Reference reference2 = new Structure.Reference(ref2.getCode(), REF_BOOK_CODE, toPlaceholder("name2"));
        structure = new Structure(Arrays.asList(id, ref1, ref2, name), Arrays.asList(reference1, reference2));

        referenceWithValueMap = new HashMap<>();
        referenceWithValueMap.put(reference1, REFERENCE_VAL1);
        referenceWithValueMap.put(reference2, REFERENCE_VAL2);

        referenceStructure = new Structure(
                Arrays.asList(
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
    public void testValidate() throws Exception {
        when(versionService.search(eq(VERSION_ID), any(SearchDataCriteria.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(VERSION_ID);
        versionEntity.setStructure(referenceStructure);
        when(versionRepository.findLastVersion(eq(REF_BOOK_CODE), eq(RefBookVersionStatus.PUBLISHED))).thenReturn(versionEntity);

        List<Message> messages = new ReferenceValueValidation(versionService, versionRepository, referenceWithValueMap, structure, Collections.singleton(REF_ATTRIBUTE_CODE2)).validate();
        Assert.assertTrue(messages.size() == 1);
        Message expected1 = new Message(ReferenceValueValidation.REFERENCE_ERROR_CODE, REF_ATTRIBUTE_NAME1, REFERENCE_VAL1);
        Assert.assertTrue(messages.contains(expected1));
    }
}
