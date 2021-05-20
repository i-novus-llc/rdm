package ru.i_novus.ms.rdm.impl.file;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.file.process.RowsValidator;
import ru.i_novus.ms.rdm.impl.file.process.RowsValidatorImpl;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.ms.rdm.impl.validation.ReferenceValueValidation;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.LinkedHashMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.impl.file.BufferedRowsPersisterTest.createTestStructure;

@RunWith(MockitoJUnitRunner.class)
public class RowsValidatorTest {

    private RowsValidator rowsValidator;

    @Mock
    private VersionService versionService;

    @Mock
    private SearchDataService searchDataService;

    private static final String ATTRIBUTE_NAME = "ref_name";

    private static final String ATTRIBUTE_VALUE = "ref_value";
    private static final String ATTRIBUTE_INVALID_VALUE = "invalid_" + ATTRIBUTE_VALUE;

    private static final String REFERRED_ATTRIBUTE = "name";

    private static final String REFERRED_CODE = "REF_CODE";
    private static final Integer REFERRED_VERSION = 1;

    @Before
    public void setUp() {

        rowsValidator = new RowsValidatorImpl(2,
                versionService, searchDataService,
                createTestStructureWithReference(), "",
                100, false, emptyList()
        );

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(REFERRED_VERSION);
        versionEntity.setRefBook(new DefaultRefBookEntity());
        versionEntity.setStructure(createTestStructure());
        when(versionService.getLastPublishedVersion(eq(REFERRED_CODE)))
                .thenReturn(ModelGenerator.versionModel(versionEntity));

        PageImpl<RefBookRowValue> searchValues = new PageImpl<>(singletonList(
                new RefBookRowValue(1L, singletonList(new StringFieldValue(REFERRED_ATTRIBUTE, ATTRIBUTE_VALUE)), null)
        ));
        when(versionService.search(eq(REFERRED_VERSION), any())).thenReturn(searchValues);
    }

    @Test
    public void testAppendAndProcess() {
        Row row = createTestRowWithReference();
        Result appendExpected = new Result(0, 0, null);
        Result processExpected = new Result(1, 1, emptyList());

        Result appendActual = rowsValidator.append(row);
        Result processActual = rowsValidator.process();

        Assert.assertEquals(appendExpected, appendActual);
        Assert.assertEquals(processExpected, processActual);
    }

    @Test
    public void testAppendAndProcessWithErrors() {
        Row validRow = createTestRowWithReference();
        Row notValidRow = new Row(new LinkedHashMap<>() {{
            put(ATTRIBUTE_NAME, new Reference(ATTRIBUTE_INVALID_VALUE, "display_" + ATTRIBUTE_INVALID_VALUE));
        }});

        rowsValidator.append(validRow);
        rowsValidator.append(notValidRow);

        try {
            rowsValidator.process();
            Assert.fail();
        } catch (UserException e) {
            Assert.assertEquals(1, e.getMessages().size());
            Assert.assertEquals(new Message(ReferenceValueValidation.REFERENCE_VALUE_NOT_FOUND_CODE_EXCEPTION_CODE, ATTRIBUTE_NAME, ATTRIBUTE_INVALID_VALUE), e.getMessages().get(0));
        }
    }

    /**
     * Проверка если у значения атритбута недопустимый тип, то остальные проверки игнорируются
     * @ throws Exception
     */
    //@Test
    //public void testIgnoreAttributeIfIsHasInvalidType() {
    //    // to-do
    //}

    private Row createTestRowWithReference() {
        return new Row(new LinkedHashMap<>() {{
            put(ATTRIBUTE_NAME, new Reference(ATTRIBUTE_VALUE, "display_" + ATTRIBUTE_VALUE));
        }});
    }

    private Structure createTestStructureWithReference() {
        Structure structure = new Structure();
        structure.setAttributes(singletonList(Structure.Attribute.build(ATTRIBUTE_NAME, ATTRIBUTE_NAME, FieldType.REFERENCE, "description")));
        structure.setReferences(singletonList(new Structure.Reference(ATTRIBUTE_NAME, REFERRED_CODE, null)));
        return structure;
    }
}
