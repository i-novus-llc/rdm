package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.file.process.RowsValidator;
import ru.inovus.ms.rdm.file.process.RowsValidatorImpl;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.validation.ReferenceValueValidation;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.inovus.ms.rdm.file.BufferedRowsPersisterTest.createTestStructure;

@RunWith(MockitoJUnitRunner.class)
public class RowsValidatorTest {

    private RowsValidator rowsValidator;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private VersionService versionService;
    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private SearchDataService searchDataService;

    private static final String ATTRIBUTE_NAME = "ref_name";

    private static final String ATTRIBUTE_NAME_WITH_WRONG_TYPE = "ref_name_wrong";

    private static final String ATTRIBUTE_VALUE = "ref_value";

    private static final String WRONG_ATTRIBUTE_VALUE = "ref_value_wrong";

    private static final String REFERENCE_ATTRIBUTE = "name";

    private static final String REFERENCE_CODE = "REF_CODE";
    private static final Integer REFERENCE_VERSION = 1;

    private SearchDataCriteria searchDataCriteria;

    @Before
    public void setUp() {
        rowsValidator = new RowsValidatorImpl(versionService, versionRepository, searchDataService, createTestStructureWithReference(), "",
                100, Collections.emptyList());
        when(fieldFactory.createField(eq(REFERENCE_ATTRIBUTE), eq(FieldType.STRING)))
                .thenReturn(new StringField(REFERENCE_ATTRIBUTE));
                //.thenReturn(new ReferenceField(REFERENCE_ATTRIBUTE));

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(REFERENCE_VERSION);
        versionEntity.setStructure(createTestStructure());
        when(versionRepository.findLastVersion(eq(REFERENCE_CODE), eq(RefBookVersionStatus.PUBLISHED))).thenReturn(versionEntity);

        AttributeFilter attributeFilter = new AttributeFilter(REFERENCE_ATTRIBUTE, ATTRIBUTE_VALUE, FieldType.STRING, SearchTypeEnum.EXACT);
        searchDataCriteria = new SearchDataCriteria(
                new HashSet<List<AttributeFilter>>() {{
                    add(Collections.singletonList(attributeFilter));
                }},
                null);
        when(versionService.search(eq(REFERENCE_VERSION), eq(searchDataCriteria)))
                .thenReturn(new PageImpl<>(Collections.singletonList(new RefBookRowValue())));
    }

    @Test
    public void testAppendAndProcess() {
        Row row = createTestRowWithReference();
        Result expected = new Result(1, 1, Collections.emptyList());

        Result appendActual = rowsValidator.append(row);
        Result processActual = rowsValidator.process();

        Assert.assertEquals(expected, appendActual);
        Assert.assertEquals(expected, processActual);
    }

    @Test
    public void testAppendAndProcessWithErrors() {
        Row validRow = createTestRowWithReference();
        String newAttributeValue = ATTRIBUTE_VALUE + "_1";
        Row notValidRow = new Row(new LinkedHashMap() {{
            put(ATTRIBUTE_NAME, new Reference(newAttributeValue, newAttributeValue));
        }});
        Result expected = new Result(1, 2, Collections.singletonList(new Message("validation.reference.err", ATTRIBUTE_NAME, newAttributeValue)));
        when(versionService.search(AdditionalMatchers.not(eq(REFERENCE_VERSION)), AdditionalMatchers.not(eq(searchDataCriteria))))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        rowsValidator.append(validRow);
        Result appendActual = rowsValidator.append(notValidRow);

        try {
            rowsValidator.process();
            Assert.fail();
        } catch (UserException e) {
            Assert.assertEquals(1, e.getMessages().size());
            Assert.assertEquals(new Message(ReferenceValueValidation.REFERENCE_ERROR_CODE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE + "_1"), e.getMessages().get(0));
        }

        Assert.assertEquals(expected, appendActual);
    }

    /**
     * Проверка если у значения атритбута недопустимый тип, то остальные проверки игнорируются
     * @throws Exception
     */
    @Test
    public void testIgnoreAttributeIfIsHasInvalidType() throws Exception {
        //todo
    }

    private Row createTestRowWithReference() {
        return new Row(new LinkedHashMap<String, Object>() {{
            put(ATTRIBUTE_NAME, new Reference(ATTRIBUTE_VALUE, ATTRIBUTE_VALUE));
        }});
    }

    private Structure createTestStructureWithReference() {
        Structure structure = new Structure();
        structure.setAttributes(Collections.singletonList(Structure.Attribute.build(ATTRIBUTE_NAME, ATTRIBUTE_NAME, FieldType.REFERENCE, "description")));
        structure.setReferences(Collections.singletonList(new Structure.Reference(ATTRIBUTE_NAME, REFERENCE_CODE, null)));
        return structure;
    }
}
