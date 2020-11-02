package ru.inovus.ms.rdm.n2o.service;

import junit.framework.TestCase;
import net.n2oapp.platform.jaxrs.RestPage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.validation.*;
import ru.inovus.ms.rdm.api.service.ConflictService;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.n2o.model.AttributeCriteria;
import ru.inovus.ms.rdm.n2o.model.FormAttribute;
import ru.inovus.ms.rdm.n2o.model.ReadAttribute;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.version.CreateAttribute;
import ru.inovus.ms.rdm.api.model.version.UpdateAttribute;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Stream.of;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;
import static ru.inovus.ms.rdm.api.model.Structure.Attribute.build;
import static ru.inovus.ms.rdm.api.model.validation.AttributeValidationType.*;

/**
 * Тестирование работы с атрибутами
 */
@RunWith(MockitoJUnitRunner.class)
public class StructureControllerTest extends TestCase {

    @InjectMocks
    private StructureController structureController;

    @Mock
    private RefBookService refBookService;
    @Mock
    private VersionService versionService;
    @Mock
    private DraftService draftService;
    @Mock
    private ConflictService conflictService;

    @Captor
    ArgumentCaptor<AttributeValidationRequest> validationRequestArgumentCaptor;
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
    private Integer referenceVersionId = -1;
    private String referenceAttribute = "count";
    private String referenceDisplayExpression = toPlaceholder(referenceAttribute);
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
    public void testReadSimple() {

        when(versionService.getStructure(eq(versionId)))
                .thenReturn(new Structure(singletonList(build(testCode, null, FieldType.INTEGER, null)), null));
        when(draftService.getAttributeValidations(eq(versionId), isNull())).thenReturn(emptyList());
        when(refBookService.getByVersionId(eq(versionId))).thenReturn(new RefBook());

        RestPage<ReadAttribute> page = structureController.getPage(new AttributeCriteria(versionId));
        ReadAttribute actual = page.getContent().get(0);

        assertEquals(testCode, actual.getCode());
        assertEquals(FieldType.INTEGER, actual.getType());
        assertValidationPartEquals(new FormAttribute(), actual);
    }

    /**
     * Тест получения атрибута со всеми проверками (без соответствия типов и проверок)
     */
    @Test
    public void testReadValidations() {
        Structure structure = new Structure(singletonList(build(testCode, null, null, null)), null);
        FormAttribute expectedValidation = createAllValidationAttribute();

        when(versionService.getStructure(eq(versionId))).thenReturn(structure);
        when(draftService.getAttributeValidations(eq(versionId), isNull())).thenReturn(expectedValidations);
        when(refBookService.getByVersionId(eq(versionId))).thenReturn(new RefBook());

        RestPage<ReadAttribute> page = structureController.getPage(new AttributeCriteria(versionId));
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
        FormAttribute formAttribute = createAllValidationAttribute();
        when(versionService.getStructure(eq(versionId))).thenReturn(new Structure());

        structureController.updateAttribute(versionId, formAttribute);

        verify(draftService, times(1)).updateAttribute(updateAttributeArgumentCaptor.capture());
        verify(draftService, times(1)).updateAttributeValidations(eq(versionId), validationRequestArgumentCaptor.capture());

        assertValidationListEquals(expectedValidations, validationRequestArgumentCaptor.getValue().getValidations());
        assertEquals(testCode, updateAttributeArgumentCaptor.getValue().getCode());
    }

    /**
     * Тест обновления атрибута со всеми проверками
     */
    @Test
    public void testCreateReference() {

        FormAttribute formAttribute = new FormAttribute();
        formAttribute.setCode(testCode);
        formAttribute.setName(testName);
        formAttribute.setIsPrimary(false);
        formAttribute.setDescription(testDescription);
        formAttribute.setType(FieldType.REFERENCE);
        formAttribute.setDisplayExpression(referenceDisplayExpression);
        formAttribute.setReferenceCode(referenceCode);
        structureController.createAttribute(versionId, formAttribute);

        verify(draftService, times(1)).createAttribute(createAttributeArgumentCaptor.capture());
        verify(draftService, times(1)).updateAttributeValidations(eq(versionId), any(AttributeValidationRequest.class));

        CreateAttribute actual = createAttributeArgumentCaptor.getValue();
        assertEquals(testCode, actual.getAttribute().getCode());
        assertEquals(testName, actual.getAttribute().getName());
        assertEquals(testDescription, actual.getAttribute().getDescription());
        assertEquals(FieldType.REFERENCE, actual.getAttribute().getType());
        assertFalse(actual.getAttribute().getIsPrimary());
        assertEquals(testCode, actual.getReference().getAttribute());
        assertEquals(referenceDisplayExpression, actual.getReference().getDisplayExpression());
        assertEquals(referenceCode, actual.getReference().getReferenceCode());
    }

    /**
     * Тест получения атрибута cо ссылочным типом
     */
    @Test
    public void testReadReference() {

        RefBook referenceRefbook = new RefBook();
        referenceRefbook.setRefBookId(refBookId);

        when(versionService.getStructure(eq(versionId)))
                .thenReturn(new Structure(
                        singletonList(build(testCode, null, FieldType.REFERENCE, null)),
                        singletonList(new Structure.Reference(testCode, referenceCode, referenceDisplayExpression))));
        when(draftService.getAttributeValidations(eq(versionId), isNull()))
                .thenReturn(emptyList());
        when(refBookService.getId(eq(referenceCode))).thenReturn(refBookId);

        RefBookVersion referenceVersion = new RefBookVersion();
        referenceVersion.setId(referenceVersionId);
        referenceVersion.setStructure(new Structure(singletonList(build(referenceAttribute, null, FieldType.INTEGER, null)), null));
        when(versionService.getLastPublishedVersion(eq(referenceCode))).thenReturn(referenceVersion);
        when(refBookService.getByVersionId(eq(versionId))).thenReturn(new RefBook(referenceVersion));

        RestPage<ReadAttribute> page = structureController.getPage(new AttributeCriteria(versionId));
        ReadAttribute actual = page.getContent().get(0);

        assertEquals(testCode, actual.getCode());
        assertEquals(FieldType.REFERENCE, actual.getType());
    }

    private FormAttribute createAllValidationAttribute() {

        FormAttribute formAttribute = new FormAttribute();
        formAttribute.setCode(testCode);
        formAttribute.setRequired(true);
        formAttribute.setUnique(true);
        formAttribute.setPlainSize(plainSize);
        formAttribute.setMinInteger(minInteger);
        formAttribute.setMaxInteger(maxInteger);
        formAttribute.setIntPartSize(intPartSize);
        formAttribute.setFracPartSize(fracPartSize);
        formAttribute.setMinFloat(minFloat);
        formAttribute.setMaxFloat(maxFloat);
        formAttribute.setMinDate(minDate);
        formAttribute.setMaxDate(maxDate);
        formAttribute.setRegExp(regExp);

        return formAttribute;
    }

    private void assertValidationPartEquals(FormAttribute expected, FormAttribute actual) {

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