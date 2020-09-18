package ru.i_novus.ms.rdm.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.api.util.RefBookTestUtils.*;

public class StructureTest {

    private static final String ID_ATTRIBUTE_CODE = "ID";
    private static final String NAME_ATTRIBUTE_CODE = "NAME";
    private static final String STRING_ATTRIBUTE_CODE = "CHAR";
    private static final String NUMBER_ATTRIBUTE_CODE = "NUMB";
    private static final String BOOLEAN_ATTRIBUTE_CODE = "BOOL";
    private static final String DATE_ATTRIBUTE_CODE = "DATE";

    private static final List<String> ATTRIBUTE_CODES = List.of(
            ID_ATTRIBUTE_CODE, NAME_ATTRIBUTE_CODE,
            STRING_ATTRIBUTE_CODE, NUMBER_ATTRIBUTE_CODE, BOOLEAN_ATTRIBUTE_CODE, DATE_ATTRIBUTE_CODE
    );

    private static final String REFERENCE_ATTRIBUTE_CODE = "REFER";
    private static final String SELF_REFER_ATTRIBUTE_CODE = "SELFY";

    private static final List<String> REFERENCE_CODES = List.of(
            REFERENCE_ATTRIBUTE_CODE, SELF_REFER_ATTRIBUTE_CODE
    );

    private static final String REFERRED_BOOK_CODE = "REFERRED";
    private static final String REFERRED_BOOK_ATTRIBUTE_CODE = "VALUE";
    private static final String SELF_REFERRED_BOOK_CODE = "SELFBOOK";
    private static final String SELF_REFERRED_BOOK_ATTRIBUTE_CODE = ID_ATTRIBUTE_CODE;
    private static final List<String> REFERRED_BOOK_CODES = List.of(
            REFERRED_BOOK_CODE, SELF_REFERRED_BOOK_CODE
    );

    private static final Structure.Attribute ID_ATTRIBUTE = Structure.Attribute.buildPrimary(
            ID_ATTRIBUTE_CODE, ID_ATTRIBUTE_CODE.toLowerCase(), FieldType.INTEGER, "primary");
    private static final Structure.Attribute NAME_ATTRIBUTE = Structure.Attribute.buildLocalizable(
            NAME_ATTRIBUTE_CODE, NAME_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "name");
    private static final Structure.Attribute STRING_ATTRIBUTE = Structure.Attribute.build(
            STRING_ATTRIBUTE_CODE, STRING_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "string");
    private static final Structure.Attribute NUMBER_ATTRIBUTE = Structure.Attribute.build(
            NUMBER_ATTRIBUTE_CODE, NUMBER_ATTRIBUTE_CODE.toLowerCase(), FieldType.INTEGER, "number");
    private static final Structure.Attribute BOOLEAN_ATTRIBUTE = Structure.Attribute.build(
            BOOLEAN_ATTRIBUTE_CODE, BOOLEAN_ATTRIBUTE_CODE.toLowerCase(), FieldType.BOOLEAN, "boolean");
    private static final Structure.Attribute DATE_ATTRIBUTE = Structure.Attribute.build(
            DATE_ATTRIBUTE_CODE, DATE_ATTRIBUTE_CODE.toLowerCase(), FieldType.DATE, "date");
    private static final Structure.Attribute REFERENCE_ATTRIBUTE = Structure.Attribute.build(
            REFERENCE_ATTRIBUTE_CODE, REFERENCE_ATTRIBUTE_CODE.toLowerCase(), FieldType.REFERENCE, "reference");
    private static final Structure.Attribute SELF_REFER_ATTRIBUTE = Structure.Attribute.build(
            SELF_REFER_ATTRIBUTE_CODE, SELF_REFER_ATTRIBUTE_CODE.toLowerCase(), FieldType.REFERENCE, "self-ref");

    private static final Structure.Reference REFERENCE = new Structure.Reference(
            REFERENCE_ATTRIBUTE_CODE, REFERRED_BOOK_CODE,
            DisplayExpression.toPlaceholder(REFERRED_BOOK_ATTRIBUTE_CODE)
    );
    private static final Structure.Reference SELF_REFER = new Structure.Reference(
            SELF_REFER_ATTRIBUTE_CODE, SELF_REFERRED_BOOK_CODE,
            DisplayExpression.toPlaceholder(SELF_REFERRED_BOOK_ATTRIBUTE_CODE)
    );

    private static final List<Structure.Attribute> ATTRIBUTE_LIST = List.of(
            ID_ATTRIBUTE, NAME_ATTRIBUTE,
            STRING_ATTRIBUTE, NUMBER_ATTRIBUTE, BOOLEAN_ATTRIBUTE, DATE_ATTRIBUTE,
            REFERENCE_ATTRIBUTE, SELF_REFER_ATTRIBUTE
    );
    private static final List<Structure.Reference> REFERENCE_LIST = List.of(
            REFERENCE, SELF_REFER
    );

    private static final String CHANGE_ATTRIBUTE_CODE = "CHANGE";
    private static final Structure.Attribute CHANGE_ATTRIBUTE = Structure.Attribute.build(
            CHANGE_ATTRIBUTE_CODE, CHANGE_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, "change");

    private static final String CHANGE_REF_ATTRIBUTE_CODE = "CHANGE_REF";
    private static final Structure.Attribute CHANGE_REF_ATTRIBUTE = Structure.Attribute.build(
            CHANGE_REF_ATTRIBUTE_CODE, CHANGE_REF_ATTRIBUTE_CODE.toLowerCase(), FieldType.REFERENCE, "change-ref");
    private static final Structure.Reference CHANGE_REF_REFERENCE = new Structure.Reference(
            CHANGE_REF_ATTRIBUTE_CODE, REFERRED_BOOK_CODE,
            DisplayExpression.toPlaceholder(REFERRED_BOOK_ATTRIBUTE_CODE)
    );

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testEmpty() {

        Structure structure = new Structure();
        assertNotNull(structure);
        assertTrue(structure.isEmpty());

        assertNotNull(structure.getAttributes());
        assertNotNull(structure.getReferences());
        assertEquals(0, structure.getAttributes().size());
        assertEquals(0, structure.getReferences().size());

        assertNull(structure.getAttribute(ID_ATTRIBUTE_CODE));
        assertNull(structure.getReference(ID_ATTRIBUTE_CODE));

        assertEquals(emptyList(), structure.getRefCodeAttributes(ID_ATTRIBUTE_CODE));
        assertEquals(emptyList(), structure.getRefCodeReferences(ID_ATTRIBUTE_CODE));

        assertFalse(structure.hasPrimary());
        structure.clearPrimary();
        assertFalse(structure.hasPrimary());

        Structure sameStructure = new Structure(null, null);
        assertObjects(Assert::assertEquals, structure, sameStructure);

        Structure cloneStructure = new Structure(structure);
        assertObjects(Assert::assertEquals, structure, cloneStructure);

        Structure copyStructure = shallowCopyStructure(structure);
        assertEquals(structure, copyStructure);
    }

    @Test
    public void testClass() {

        Structure structure = createStructure();
        assertEquals(ATTRIBUTE_LIST, structure.getAttributes());
        assertEquals(REFERENCE_LIST, structure.getReferences());

        Structure emptyStructure = new Structure();
        assertObjects(Assert::assertNotEquals, structure, emptyStructure);

        Structure cloneStructure = new Structure(structure);
        assertEquals(structure, cloneStructure);
        assertTrue(cloneStructure.storageEquals(structure));

        Structure copyStructure = shallowCopyStructure(structure);
        assertEquals(structure, copyStructure);
    }

    @Test
    public void testCopy() {

        Structure structure = createStructure();

        Structure copyStructure = new Structure(structure);
        assertEquals(structure, copyStructure);
        assertTrue(copyStructure.storageEquals(structure));

        structure.getAttributes().forEach(attribute -> {
            Structure.Attribute copyAttribute = copyAttribute(attribute);
            assertObjects(Assert::assertEquals, attribute, copyAttribute);
            assertTrue(copyAttribute.storageEquals(attribute));
        });

        structure.getReferences().forEach(reference -> {
            Structure.Reference copyReference = copyReference(reference);
            assertObjects(Assert::assertEquals, reference, copyReference);
        });
    }

    @Test
    public void testProcessPrimary() {

        Structure structure = createStructure();

        assertTrue(structure.hasPrimary());
        List<Structure.Attribute> primaries = structure.getPrimaries();
        assertNotNull(primaries);
        assertEquals(1, primaries.size());

        Structure.Attribute primary = primaries.get(0);
        assertObjects(Assert::assertEquals, ID_ATTRIBUTE, primary);

        structure.clearPrimary();
        primaries = structure.getPrimaries();
        assertNotNull(primaries);
        assertEquals(0, primaries.size());
        assertFalse(primary.hasIsPrimary());

        primary.setIsPrimary(Boolean.TRUE);
        primaries = structure.getPrimaries();
        assertNotNull(primaries);
        assertEquals(1, primaries.size());
    }

    @Test
    public void testProcessLocalizable() {

        Structure structure = createStructure();
        List<Structure.Attribute> localizables = structure.getLocalizables();
        assertNotNull(localizables);
        assertEquals(1, localizables.size());
        assertObjects(Assert::assertEquals, NAME_ATTRIBUTE, localizables.get(0));
    }

    @Test
    public void testGetByCode() {

        Structure structure = createStructure();

        IntStream.range(0, ATTRIBUTE_CODES.size()).forEach(index -> {

            String code = ATTRIBUTE_CODES.get(index);
            assertObjects(Assert::assertEquals,
                    ATTRIBUTE_LIST.get(index),
                    structure.getAttribute(code)
            );
        });

        IntStream.range(0, REFERENCE_CODES.size()).forEach(index -> {

            String code = REFERENCE_CODES.get(index);

            assertObjects(Assert::assertEquals,
                    ATTRIBUTE_LIST.stream()
                            .filter(attribute -> code.equals(attribute.getCode()))
                            .findFirst().orElse(null),
                    structure.getAttribute(code)
            );
            assertObjects(Assert::assertEquals,
                    REFERENCE_LIST.get(index),
                    structure.getReference(code)
            );
        });
    }

    @Test
    public void testGetByRefCode() {

        Structure structure = createStructure();

        IntStream.range(0, REFERRED_BOOK_CODES.size()).forEach(index -> {

            String refCode = REFERRED_BOOK_CODES.get(index);
            Structure.Reference reference = REFERENCE_LIST.get(index);
            assertFalse(reference.isNull());

            List<Structure.Reference> references = structure.getRefCodeReferences(refCode);
            assertNotNull(references);
            assertEquals(1, references.size());
            assertObjects(Assert::assertEquals, reference, references.get(0));

            List<Structure.Attribute> attributes = structure.getRefCodeAttributes(refCode);
            assertNotNull(attributes);
            assertEquals(1, attributes.size());

            Structure.Attribute referenceAttribute = ATTRIBUTE_LIST.stream()
                    .filter(attribute -> reference.getAttribute().equals(attribute.getCode()))
                    .findFirst().orElse(null);
            assertNotNull(referenceAttribute);
            assertTrue(referenceAttribute.isReferenceType());
            assertObjects(Assert::assertEquals, referenceAttribute, attributes.get(0));
        });
    }

    @Test
    public void testChangeWithNull() {

        Structure oldStructure = new Structure();

        Structure addedStructure = new Structure(oldStructure);

        addedStructure.add(CHANGE_ATTRIBUTE, CHANGE_REF_REFERENCE);
        assertEquals(1, addedStructure.getAttributes().size());
        assertEquals(0, addedStructure.getReferences().size());

        addedStructure = new Structure(oldStructure);

        addedStructure.add(CHANGE_REF_ATTRIBUTE, CHANGE_REF_REFERENCE);
        assertEquals(1, addedStructure.getAttributes().size());
        assertEquals(1, addedStructure.getReferences().size());

        Structure updatedStructure = new Structure(addedStructure);

        updatedStructure.update(CHANGE_REF_REFERENCE, null);
        assertObjects(Assert::assertNotEquals, addedStructure, updatedStructure);
        assertTrue(updatedStructure.storageEquals(addedStructure));

        assertEquals(1, updatedStructure.getAttributes().size());
        assertEquals(0, updatedStructure.getReferences().size());

        Structure removedStructure = new Structure(updatedStructure);

        removedStructure.remove(null);
        assertEquals(1, removedStructure.getAttributes().size());
        assertEquals(0, removedStructure.getReferences().size());

        removedStructure = new Structure(addedStructure);
        removedStructure.getAttribute(CHANGE_REF_ATTRIBUTE_CODE).setCode(CHANGE_ATTRIBUTE_CODE);
        removedStructure.remove(CHANGE_ATTRIBUTE_CODE);
        assertEquals(0, removedStructure.getAttributes().size());
        assertEquals(1, removedStructure.getReferences().size());
    }

    @Test
    public void testChangeWithEmptyCode() {

        Structure oldStructure = new Structure();

        Structure addedStructure = new Structure(oldStructure);

        Structure.Attribute emptyCodedAttribute = copyAttribute(NAME_ATTRIBUTE);
        emptyCodedAttribute.setCode(null);

        addedStructure.add(emptyCodedAttribute, null);
        assertEquals(0, addedStructure.getAttributes().size());
        assertEquals(0, addedStructure.getReferences().size());

        addedStructure.add(CHANGE_ATTRIBUTE, null);
        assertEquals(1, addedStructure.getAttributes().size());
        assertEquals(0, addedStructure.getReferences().size());

        addedStructure.update(CHANGE_ATTRIBUTE, emptyCodedAttribute);
        assertEquals(1, addedStructure.getAttributes().size());
        assertEquals(0, addedStructure.getReferences().size());

        addedStructure = new Structure(oldStructure);

        addedStructure.add(CHANGE_REF_ATTRIBUTE, CHANGE_REF_REFERENCE);
        assertEquals(1, addedStructure.getAttributes().size());
        assertEquals(1, addedStructure.getReferences().size());

        Structure updatedStructure = new Structure(addedStructure);

        Structure.Reference emptyCodedReference = copyReference(CHANGE_REF_REFERENCE);
        emptyCodedReference.setAttribute(null);

        updatedStructure.update(CHANGE_REF_REFERENCE, emptyCodedReference);
        assertEquals(1, addedStructure.getAttributes().size());
        assertEquals(1, addedStructure.getReferences().size());
    }

    @Test
    public void testChangeEmptyWithReference() {

        Structure oldStructure = new Structure();
        changeStructureWithReference(oldStructure);
    }

    @Test
    public void testChangeFilledWithAttribute() {

        Structure oldStructure = createStructure();
        changeStructureWithAttribute(oldStructure);
    }

    @Test
    public void testChangeEmptyWithAttribute() {

        Structure oldStructure = new Structure();
        changeStructureWithAttribute(oldStructure);
    }

    @Test
    public void testChangeFilledWithReference() {

        Structure oldStructure = createStructure();
        changeStructureWithReference(oldStructure);
    }

    @Test
    public void testFindReferenceAttribute() {

        Structure structure = createStructure();

        Structure.Attribute referenceAttribute = REFERENCE.findReferenceAttribute(structure);
        assertEquals(ID_ATTRIBUTE, referenceAttribute);

        try {
            REFERENCE.findReferenceAttribute(new Structure());
            fail(getFailedMessage(UserException.class));

        } catch (UserException e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("primary.attribute.not.found", getExceptionMessage(e));
        }

        structure.getAttribute(NAME_ATTRIBUTE_CODE).setIsPrimary(Boolean.TRUE);
        try {
            REFERENCE.findReferenceAttribute(structure);
            fail(getFailedMessage(UserException.class));

        } catch (UserException e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("primary.attribute.is.multiple", getExceptionMessage(e));
        }
    }

    /** Создание структуры с глубоким копированием атрибутов и ссылок. */
    private Structure createStructure() {

        Structure structure = new Structure(ATTRIBUTE_LIST, REFERENCE_LIST);
        return new Structure(structure);
    }

    /** Поверхностное копирование структуры. */
    private Structure shallowCopyStructure(Structure structure) {

        Structure result = new Structure();

        result.setAttributes(structure.getAttributes());
        result.setReferences(structure.getReferences());

        return result;
    }

    private Structure.Attribute copyAttribute(Structure.Attribute attribute) {

        Structure.Attribute result = new Structure.Attribute();

        result.setCode(attribute.getCode());
        result.setName(attribute.getName());
        result.setType(attribute.getType());

        result.setIsPrimary(attribute.getIsPrimary());
        result.setLocalizable(attribute.getLocalizable());

        result.setDescription(attribute.getDescription());

        return result;
    }

    private Structure.Reference copyReference(Structure.Reference reference) {

        Structure.Reference result = new Structure.Reference();

        result.setAttribute(reference.getAttribute());
        result.setReferenceCode(reference.getReferenceCode());
        result.setDisplayExpression(reference.getDisplayExpression());

        return result;
    }

    private void changeStructureWithAttribute(Structure oldStructure) {

        Structure addedStructure = new Structure(oldStructure);

        addedStructure.add(null, null);
        assertObjects(Assert::assertEquals, oldStructure, addedStructure);
        assertTrue(addedStructure.storageEquals(oldStructure));

        addedStructure.add(copyAttribute(CHANGE_ATTRIBUTE), null);
        assertObjects(Assert::assertNotEquals, oldStructure, addedStructure);
        assertFalse(addedStructure.storageEquals(oldStructure));

        assertEquals(oldStructure.getAttributes().size() + 1, addedStructure.getAttributes().size());
        assertEquals(oldStructure.getReferences().size(), addedStructure.getReferences().size());

        Structure.Attribute addedAttribute = addedStructure.getAttribute(CHANGE_ATTRIBUTE_CODE);
        Structure.Reference addedReference = addedStructure.getReference(CHANGE_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, CHANGE_ATTRIBUTE, addedAttribute);
        assertNull(addedReference);

        Structure updatedStructure = new Structure(addedStructure);

        Structure.Attribute updatingAttribute = copyAttribute(CHANGE_ATTRIBUTE);
        String newName = updatingAttribute.getName() + "_updated";
        updatingAttribute.setName(newName);
        updatingAttribute.setType(FieldType.REFERENCE);

        updatedStructure.update(addedAttribute, updatingAttribute);
        assertFalse(updatedStructure.storageEquals(oldStructure));

        Structure.Attribute updatedAttribute = updatedStructure.getAttribute(CHANGE_ATTRIBUTE_CODE);
        assertEquals(newName, updatedAttribute.getName());
        assertObjects(Assert::assertNotEquals, CHANGE_ATTRIBUTE, updatedAttribute);

        Structure.Reference updatingReference = copyReference(CHANGE_REF_REFERENCE);
        updatingReference.setAttribute(CHANGE_ATTRIBUTE_CODE);
        String newDisplayExpression = updatingReference.getDisplayExpression() + "_updated";
        updatingReference.setDisplayExpression(newDisplayExpression);
        updatedStructure.update(addedReference, updatingReference);

        Structure.Reference updatedReference = updatedStructure.getReference(CHANGE_ATTRIBUTE_CODE);
        assertEquals(newDisplayExpression, updatedReference.getDisplayExpression());
        assertObjects(Assert::assertNotEquals, CHANGE_REF_REFERENCE, updatedReference);

        Structure removedStructure = new Structure(updatedStructure);

        removedStructure.remove(null);
        assertObjects(Assert::assertEquals, updatedStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(updatedStructure));

        removedStructure.remove(CHANGE_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, oldStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(oldStructure));
    }

    private void changeStructureWithReference(Structure oldStructure) {

        Structure addedStructure = new Structure(oldStructure);

        addedStructure.add(null, CHANGE_REF_REFERENCE);
        assertObjects(Assert::assertEquals, oldStructure, addedStructure);
        assertTrue(addedStructure.storageEquals(oldStructure));

        assertEquals(oldStructure.getAttributes().size(), addedStructure.getAttributes().size());
        assertEquals(oldStructure.getReferences().size(), addedStructure.getReferences().size());

        addedStructure.add(copyAttribute(CHANGE_REF_ATTRIBUTE), copyReference(CHANGE_REF_REFERENCE));
        assertObjects(Assert::assertNotEquals, oldStructure, addedStructure);
        assertFalse(addedStructure.storageEquals(oldStructure));

        assertEquals(oldStructure.getAttributes().size() + 1, addedStructure.getAttributes().size());
        assertEquals(oldStructure.getReferences().size() + 1, addedStructure.getReferences().size());

        Structure.Attribute addedAttribute = addedStructure.getAttribute(CHANGE_REF_ATTRIBUTE_CODE);
        Structure.Reference addedReference = addedStructure.getReference(CHANGE_REF_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, CHANGE_REF_ATTRIBUTE, addedAttribute);
        assertObjects(Assert::assertEquals, CHANGE_REF_REFERENCE, addedReference);

        Structure updatedStructure = new Structure(addedStructure);

        Structure.Attribute updatingAttribute = copyAttribute(CHANGE_REF_ATTRIBUTE);
        String newName = updatingAttribute.getName() + "_updated";
        updatingAttribute.setName(newName);

        updatedStructure.update(addedAttribute, updatingAttribute);
        assertFalse(updatedStructure.storageEquals(oldStructure));

        Structure.Attribute updatedAttribute = updatedStructure.getAttribute(CHANGE_REF_ATTRIBUTE_CODE);
        assertEquals(newName, updatedAttribute.getName());
        assertObjects(Assert::assertNotEquals, CHANGE_REF_ATTRIBUTE, updatedAttribute);

        Structure.Reference updatingReference = copyReference(CHANGE_REF_REFERENCE);
        String newDisplayExpression = updatingReference.getDisplayExpression() + "_updated";
        updatingReference.setDisplayExpression(newDisplayExpression);
        updatedStructure.update(addedReference, updatingReference);

        Structure.Reference updatedReference = updatedStructure.getReference(CHANGE_REF_ATTRIBUTE_CODE);
        assertEquals(newDisplayExpression, updatedReference.getDisplayExpression());
        assertObjects(Assert::assertNotEquals, CHANGE_REF_REFERENCE, updatedReference);

        Structure removedStructure = new Structure(updatedStructure);

        removedStructure.remove(null);
        assertObjects(Assert::assertEquals, updatedStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(updatedStructure));

        removedStructure.remove(CHANGE_REF_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, oldStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(oldStructure));
    }
}