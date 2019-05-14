package ru.inovus.ms.rdm.service;

import junit.framework.TestCase;
import net.n2oapp.platform.jaxrs.RestPage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.validation.*;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Stream.of;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;
import static ru.inovus.ms.rdm.model.Structure.Attribute.build;
import static ru.inovus.ms.rdm.model.validation.AttributeValidationType.*;

/**
 * Тестирование работы с атрибутами
 */
@RunWith(MockitoJUnitRunner.class)
public class StructureControllerTest extends TestCase {

    @InjectMocks
    private StructureController structureController;

    @Mock
    private DraftService draftService;
    @Mock
    private VersionService versionService;
    @Mock
    private RefBookService refBookService;
    @Captor
    ArgumentCaptor<List<AttributeValidation>> validationsArgumentCaptor;
    @Captor
    ArgumentCaptor<UpdateAttribute> updateAttributeArgumentCaptor;
    @Captor
    ArgumentCaptor<CreateAttribute> createAttributeArgumentCaptor;

    private final int refBookId = -2;
    private final int versionId = 15;
    private String testCode = "test_code";
    private String testName = "testName";
    private String testDescription = "testDescription";
    private String referenceCode = "test_storage";
    private Integer referenceVersion = -1;
    private String referenceAttribute = "count";
    private String displayExpression = toPlaceholder(referenceAttribute);
    private int plainSize = 2;
    private BigInteger minInteger = BigInteger.valueOf(5L);
    private BigInteger maxInteger = BigInteger.valueOf(6L);
    private int intPartSize = 3;
    private int fracPartSize = 4;
    private BigDecimal minFloat = BigDecimal.ONE;
    private BigDecimal maxFloat = BigDecimal.TEN;
    private LocalDate minDate = LocalDate.MIN;
    private LocalDate maxDate = LocalDate.MAX;
    private String regExp = ".*";


    private List<AttributeValidation> expectedValidations;

    @Before
    public void init() {
        expectedValidations = of(
                new RequiredAttributeValidation(),
                new UniqueAttributeValidation(),
                new PlainSizeAttributeValidation(plainSize),
                new FloatSizeAttributeValidation(intPartSize, fracPartSize),
                new IntRangeAttributeValidation(minInteger, maxInteger),
                new FloatRangeAttributeValidation(minFloat, maxFloat),
                new DateRangeAttributeValidation(minDate, maxDate),
                new RegExpAttributeValidation(regExp))
                .peek(v -> v.setAttribute(testCode)).collect(Collectors.toList());
    }


    /**
     * Тест получения атрибута без проверок
     */
    @Test
    public void testReadSimple() throws Exception {

        when(versionService.getStructure(eq(versionId)))
                .thenReturn(new Structure(singletonList(build(testCode, null, FieldType.INTEGER, null)), null));
        when(draftService.getAttributeValidations(eq(versionId), isNull(String.class))).thenReturn(emptyList());

        RestPage<ReadAttribute> page = structureController.getPage(new AttributeCriteria(null, versionId));
        ReadAttribute actual = page.getContent().get(0);

        assertEquals(testCode, actual.getCode());
        assertEquals(FieldType.INTEGER, actual.getType());
        assertValidationPartEquals(new Attribute(), actual);
    }

    /**
     * Тест получения атрибута со всеми проверками (без соответствия типов и проверок)
     */
    @Test
    public void testReadValidations() throws Exception {
        Structure structure = new Structure(singletonList(build(testCode, null, null, null)), null);
        Attribute expectedValidation = createAllValidationAttribute();

        when(versionService.getStructure(eq(versionId))).thenReturn(structure);
        when(draftService.getAttributeValidations(eq(versionId), isNull(String.class))).thenReturn(expectedValidations);

        RestPage<ReadAttribute> page = structureController.getPage(new AttributeCriteria(null, versionId));
        ReadAttribute actual = page.getContent().get(0);

        assertEquals(1, page.getTotalElements());
        assertEquals(testCode, actual.getCode());
        assertValidationPartEquals(expectedValidation, actual);
    }

    /**
     * Тест обновления атрибута со всеми проверками
     */
    @Test
    public void testUpdateValidations() throws Exception {
        Attribute attribute = createAllValidationAttribute();
        when(versionService.getStructure(eq(versionId))).thenReturn(new Structure());

        structureController.updateAttribute(versionId, attribute);

        verify(draftService, times(1)).updateAttribute(updateAttributeArgumentCaptor.capture());
        verify(draftService, times(1)).updateAttributeValidations(eq(versionId), eq(testCode), validationsArgumentCaptor.capture());

        assertValidationListEquals(expectedValidations, validationsArgumentCaptor.getValue());
        assertEquals(testCode, updateAttributeArgumentCaptor.getValue().getCode());

    }

    /**
     * Тест обновления атрибута со всеми проверками
     */
    @Test
    public void testCreateReference() throws Exception {
        Attribute attribute = new Attribute();
        attribute.setCode(testCode);
        attribute.setName(testName);
        attribute.setIsPrimary(false);
        attribute.setDescription(testDescription);
        attribute.setType(FieldType.REFERENCE);
        attribute.setReferenceDisplayExpression(displayExpression);
        attribute.setReferenceAttribute(referenceAttribute);
        attribute.setReferenceCode(referenceCode);
        structureController.createAttribute(versionId, attribute);

        verify(draftService, times(1)).createAttribute(createAttributeArgumentCaptor.capture());
        verify(draftService, times(1)).updateAttributeValidations(eq(versionId), eq(testCode), eq(emptyList()));

        CreateAttribute actual = createAttributeArgumentCaptor.getValue();
        assertEquals(testCode, actual.getAttribute().getCode());
        assertEquals(testName, actual.getAttribute().getName());
        assertEquals(testDescription, actual.getAttribute().getDescription());
        assertEquals(FieldType.REFERENCE, actual.getAttribute().getType());
        assertFalse(actual.getAttribute().getIsPrimary());
        assertEquals(testCode, actual.getReference().getAttribute());
        assertEquals(displayExpression, actual.getReference().getDisplayExpression());
        assertEquals(referenceAttribute, actual.getReference().getReferenceAttribute());
        assertEquals(referenceCode, actual.getReference().getReferenceCode());

    }

    /**
     * Тест получения атрибута cо ссылочным типом
     */
    @Test
    public void testReadReference() throws Exception {

        RefBook referenceRefbook = new RefBook();
        referenceRefbook.setRefBookId(refBookId);

        when(versionService.getStructure(eq(versionId)))
                .thenReturn(new Structure(
                        singletonList(build(testCode, null, FieldType.REFERENCE, null)),
                        singletonList(new Structure.Reference(testCode, referenceCode, referenceAttribute, displayExpression))));
        when(draftService.getAttributeValidations(eq(versionId), isNull(String.class))).thenReturn(emptyList());

        when(refBookService.getId(eq(referenceCode))).thenReturn(refBookId);

        RefBookVersion version = new RefBookVersion();
        version.setId(referenceVersion);
        when(versionService.getLastPublishedVersion(eq(referenceCode))).thenReturn(version);

        when(versionService.getStructure(referenceVersion))
                .thenReturn(new Structure(singletonList(build(referenceAttribute, null, FieldType.INTEGER, null)), null));

        RestPage<ReadAttribute> page = structureController.getPage(new AttributeCriteria(null, versionId));
        ReadAttribute actual = page.getContent().get(0);

        assertEquals(testCode, actual.getCode());
        assertEquals(FieldType.REFERENCE, actual.getType());
    }


    private Attribute createAllValidationAttribute() {
        Attribute attribute = new Attribute();
        attribute.setCode(testCode);
        attribute.setRequired(true);
        attribute.setUnique(true);
        attribute.setPlainSize(plainSize);
        attribute.setMinInteger(minInteger);
        attribute.setMaxInteger(maxInteger);
        attribute.setIntPartSize(intPartSize);
        attribute.setFracPartSize(fracPartSize);
        attribute.setMinFloat(minFloat);
        attribute.setMaxFloat(maxFloat);
        attribute.setMinDate(minDate);
        attribute.setMaxDate(maxDate);
        attribute.setRegExp(regExp);
        return attribute;
    }

    private void assertValidationPartEquals(Attribute expected, Attribute actual) {
        assertEquals(expected.getRequired(), actual.getRequired());
        assertEquals(expected.getUnique(), actual.getUnique());
        assertEquals(expected.getPlainSize(), actual.getPlainSize());
        assertEquals(expected.getIntPartSize(), actual.getIntPartSize());
        assertEquals(expected.getFracPartSize(), actual.getFracPartSize());
        assertEquals(expected.getMinInteger(), actual.getMinInteger());
        assertEquals(expected.getMaxInteger(), actual.getMaxInteger());
        assertEquals(expected.getMinFloat(), actual.getMinFloat());
        assertEquals(expected.getMaxFloat(), actual.getMaxFloat());
        assertEquals(expected.getMinDate(), actual.getMinDate());
        assertEquals(expected.getMaxDate(), actual.getMaxDate());
        assertEquals(expected.getRegExp(), actual.getRegExp());
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
