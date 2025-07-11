package ru.i_novus.ms.rdm.rest;

import net.n2oapp.platform.jaxrs.RestException;
import net.n2oapp.platform.test.autoconfigure.DefinePort;
import net.n2oapp.platform.test.autoconfigure.pg.EnableTestcontainersPg;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateDataRequest;
import ru.i_novus.ms.rdm.api.model.validation.*;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersionAttribute;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.rest.autoconfigure.config.BackendConfiguration;
import ru.i_novus.ms.rdm.service.Application;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.math.BigInteger.valueOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static ru.i_novus.ms.rdm.api.model.Structure.Attribute.build;
import static ru.i_novus.ms.rdm.api.model.validation.AttributeValidationType.*;
import static ru.i_novus.ms.rdm.impl.validation.resolver.IntRangeAttributeValidationResolver.INT_RANGE_EXCEPTION_CODE;
import static ru.i_novus.platform.datastorage.temporal.enums.FieldType.*;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "cxf.jaxrs.client.classes-scan=true",
                "cxf.jaxrs.client.classes-scan-packages=ru.i_novus.ms.rdm.api.rest, ru.i_novus.ms.rdm.api.service",
                "cxf.jaxrs.client.address=http://localhost:${server.port}/rdm/api",
                "fileStorage.root=src/test/resources/rdm/temp",
                "i18n.global.enabled=false",
                "rdm.enable.publish.topic=false",
                "rdm.audit.disabledActions=all",
                "management.tracing.enabled=false"
        })
@DefinePort
@EnableTestcontainersPg
@ActiveProfiles("test")
@Import(BackendConfiguration.class)
public class CustomValidationTest {

    @Autowired
    @Qualifier("refBookServiceJaxRsProxyClient")
    private RefBookService refBookService;

    @Autowired
    @Qualifier("draftRestServiceJaxRsProxyClient")
    private DraftRestService draftService;

    private final String STRING_ATTR = "stringAttr";
    private final String INTEGER_ATTR = "integerAttr";
    private final String FLOAT_ATTR = "floatAttr";
    private final String DATE_ATTR = "dateAttr";

    /**
     * Проверка добавления и удаления проверки
     */
    @Test
    public void testAddDeleteValidation() {

        String REF_BOOK_NAME = "CustomValidationTest";
        RefBook refBook = refBookService.create(new RefBookCreateRequest(REF_BOOK_NAME, null, null, null));

        Draft draft = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), createStructure()));
        final Integer draftId = draft.getId();

        // Добавление проверки
        draftService.addAttributeValidation(draftId, INTEGER_ATTR,
                new IntRangeAttributeValidation(
                        valueOf(-5),
                        valueOf(4)
                ));

        // Правильная строка
        Row validRow = new Row(Map.of(
            STRING_ATTR, "test1",
            INTEGER_ATTR, 3
        ));
        draftService.updateData(draftId, new UpdateDataRequest(null, validRow));

        // Неправильная строка
        final Row invalidRow = new Row(Map.of(
                STRING_ATTR, "test1",
                INTEGER_ATTR, 6
        ));
        UpdateDataRequest request = new UpdateDataRequest(null, invalidRow);
        try {
            draftService.updateData(draftId, request);
            fail();

        } catch (RestException e) {
            assertEquals(INT_RANGE_EXCEPTION_CODE, e.getErrors().get(0).getMessage());
        }

        // Удаление проверки
        draftService.deleteAttributeValidation(draftId, INTEGER_ATTR, AttributeValidationType.INT_RANGE);

        // Ввод той же строки после удаления
        draftService.updateData(draftId, request);
    }

    /**
     * добавление проверки на обязательность, затем замена проверки на другие.
     * ожидается удаление проверки и добавление довых
     */
    @Test
    public void testUpdateValidation() {
        String REF_BOOK_NAME = "CustomValidationUpdateTest";
        RefBook refBook = refBookService.create(new RefBookCreateRequest(REF_BOOK_NAME, null, null, null));
        Draft draft = draftService.create(new CreateDraftRequest(refBook.getRefBookId(), createStructure()));

        RefBookVersionAttribute versionAttribute = new RefBookVersionAttribute(draft.getId(), new Structure.Attribute(), new Structure.Reference());
        versionAttribute.getAttribute().setCode(INTEGER_ATTR);

        //добавление проверки на обязательность
        RequiredAttributeValidation expectedRequired = new RequiredAttributeValidation();
        draftService.updateAttributeValidations(draft.getId(), new AttributeValidationRequest(null, versionAttribute, singletonList(expectedRequired)));
        expectedRequired.setVersionId(draft.getId());
        expectedRequired.setAttribute(INTEGER_ATTR);

        List<AttributeValidation> actual = draftService.getAttributeValidations(draft.getId(), INTEGER_ATTR);
        assertEquals(1, actual.size());
        assertValidationEquals(expectedRequired, actual.get(0));

        //обновление проверки
        List<AttributeValidation> expectedValidations =
                asList(new PlainSizeAttributeValidation(5), new IntRangeAttributeValidation(valueOf(-5), valueOf(4)));
        draftService.updateAttributeValidations(draft.getId(), new AttributeValidationRequest(null, versionAttribute, expectedValidations));

        for (AttributeValidation expectedValidation : expectedValidations) {
            expectedValidation.setVersionId(draft.getId());
            expectedValidation.setAttribute(INTEGER_ATTR);
        }
        actual = draftService.getAttributeValidations(draft.getId(), INTEGER_ATTR);
        assertEquals(2, actual.size());
        assertValidationListEquals(expectedValidations, actual);

    }

    private Structure createStructure() {
        return new Structure(asList(
                build(STRING_ATTR, null, STRING, null),
                build(INTEGER_ATTR, null, INTEGER, null),
                build(FLOAT_ATTR, null, FLOAT, null),
                build(DATE_ATTR, null, DATE, null)), null);
    }

    private void assertValidationListEquals(List<AttributeValidation> expected, List<AttributeValidation> actual) {
        Iterator<AttributeValidation> actualIterator = actual.iterator();
        for (AttributeValidation validation : expected) {
            assertValidationEquals(validation, actualIterator.next());
        }
    }

    private void assertValidationEquals(AttributeValidation expected, AttributeValidation actual) {
        switch (expected.getType()) {
            case REQUIRED:
                assertEquals(REQUIRED, actual.getType());
                break;
            case UNIQUE:
                assertEquals(UNIQUE, actual.getType());
                break;
            case PLAIN_SIZE:
                assertEquals(PLAIN_SIZE, actual.getType());
                assertEquals(((PlainSizeAttributeValidation) expected).getSize(), ((PlainSizeAttributeValidation) expected).getSize());
                break;
            case FLOAT_SIZE:
                assertEquals(FLOAT_SIZE, actual.getType());
                assertEquals(((FloatSizeAttributeValidation) expected).getIntPartSize(), ((FloatSizeAttributeValidation) expected).getIntPartSize());
                assertEquals(((FloatSizeAttributeValidation) expected).getFracPartSize(), ((FloatSizeAttributeValidation) expected).getFracPartSize());
                break;
            case INT_RANGE:
                assertEquals(INT_RANGE, actual.getType());
                assertEquals(((IntRangeAttributeValidation) expected).getMin(), ((IntRangeAttributeValidation) expected).getMin());
                assertEquals(((IntRangeAttributeValidation) expected).getMax(), ((IntRangeAttributeValidation) expected).getMax());
                break;
            case FLOAT_RANGE:
                assertEquals(FLOAT_RANGE, actual.getType());
                assertEquals(((FloatRangeAttributeValidation) expected).getMin(), ((FloatRangeAttributeValidation) expected).getMin());
                assertEquals(((FloatRangeAttributeValidation) expected).getMax(), ((FloatRangeAttributeValidation) expected).getMax());
                break;
            case DATE_RANGE:
                assertEquals(DATE_RANGE, actual.getType());
                assertEquals(((DateRangeAttributeValidation) expected).getMin(), ((DateRangeAttributeValidation) expected).getMin());
                assertEquals(((DateRangeAttributeValidation) expected).getMax(), ((DateRangeAttributeValidation) expected).getMax());
                break;
            case REG_EXP:
                assertEquals(REG_EXP, actual.getType());
                assertEquals(((RegExpAttributeValidation) expected).getRegExp(), ((RegExpAttributeValidation) expected).getRegExp());
                break;
            default:
                fail();
        }
    }


}