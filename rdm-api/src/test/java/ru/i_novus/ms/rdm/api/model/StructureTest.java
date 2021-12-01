package ru.i_novus.ms.rdm.api.model;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.api.util.StructureTestConstants.*;

@SuppressWarnings("java:S5778")
public class StructureTest extends BaseTest {

    @Test
    public void testEmpty() {

        Structure structure = new Structure();
        assertNotNull(structure);
        assertObjects(Assert::assertEquals, Structure.EMPTY, structure);

        assertTrue(structure.isEmpty());
        assertSpecialEquals(structure);

        assertNotNull(structure.getAttributes());
        assertNotNull(structure.getReferences());
        assertEquals(0, structure.getAttributes().size());
        assertEquals(0, structure.getReferences().size());

        assertNull(structure.getAttribute(ID_ATTRIBUTE_CODE));
        assertNull(structure.getReference(ID_ATTRIBUTE_CODE));

        assertEmpty(structure.getRefCodeAttributes(ID_ATTRIBUTE_CODE));
        assertEmpty(structure.getRefCodeReferences(ID_ATTRIBUTE_CODE));

        assertFalse(structure.hasPrimary());
        structure.clearPrimary();
        assertFalse(structure.hasPrimary());
    }

    @Test
    public void testEmptyToString() {

        Structure structure = new Structure();
        assertNotNull(structure.toString());
    }

    @Test
    public void testEmptyByEmpty() {

        Structure structure = new Structure();

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

        structure.getAttributes().forEach(this::assertSpecialEquals);
        structure.getReferences().forEach(this::assertSpecialEquals);

        Structure emptyStructure = new Structure();
        assertObjects(Assert::assertNotEquals, structure, emptyStructure);

        Structure cloneStructure = new Structure(structure);
        assertEquals(structure, cloneStructure);

        Structure copyStructure = shallowCopyStructure(structure);
        assertEquals(structure, copyStructure);
    }

    @Test
    public void testStorageEquals() {

        Structure structure = createStructure();
        assertFalse(structureStorageEquals(structure, null));
        assertTrue(structureStorageEquals(structure, structure));

        List<Structure.Attribute> attributes = structure.getAttributes();
        assertFalse(attributeStorageEquals(attributes.get(0), null));
        assertTrue(attributeStorageEquals(attributes.get(0), attributes.get(0)));
        assertFalse(attributeStorageEquals(attributes.get(0), attributes.get(1)));

        Structure cloneStructure = new Structure(structure);
        assertTrue(structureStorageEquals(cloneStructure, structure));

        // Изменение описания атрибута:
        int changedIndex = cloneStructure.getAttributes().indexOf(NAME_ATTRIBUTE);
        Structure.Attribute changedAttribute = Structure.Attribute.build(NAME_ATTRIBUTE);
        changedAttribute.setDescription("name field");
        assertTrue(attributeStorageEquals(NAME_ATTRIBUTE, changedAttribute));

        cloneStructure.getAttributes().set(changedIndex, changedAttribute);
        assertTrue(structureStorageEquals(cloneStructure, structure));

        // Изменение кода атрибута:
        changedAttribute.setCode(changedAttribute.getCode() + "_changed");
        assertFalse(attributeStorageEquals(NAME_ATTRIBUTE, changedAttribute));

        assertFalse(structureStorageEquals(cloneStructure, structure));
    }

    private boolean structureStorageEquals(Structure structure, Structure that) {
        return structure.storageEquals(that); // Косвенный вызов для скрытия замечания о @NotNull
    }

    private boolean attributeStorageEquals(Structure.Attribute attribute, Structure.Attribute that) {
        return attribute.storageEquals(that); // Косвенный вызов для скрытия замечания о @NotNull
    }

    @Test
    public void testSomeFieldEquals() {

        Structure structure = createStructure();

        Structure cloneStructure = new Structure(structure);

        int changedIndex = cloneStructure.getAttributes().indexOf(NAME_ATTRIBUTE);
        Structure.Attribute changedAttribute = Structure.Attribute.build(NAME_ATTRIBUTE);
        assertEquals(NAME_ATTRIBUTE, changedAttribute);

        // Изменение описания атрибута:
        changedAttribute.setDescription("name field");
        assertNotEquals(NAME_ATTRIBUTE, changedAttribute);

        cloneStructure.getAttributes().set(changedIndex, changedAttribute);
        assertNotEquals(structure, cloneStructure);
    }

    @Test
    public void testCopy() {

        Structure structure = createStructure();
        assertSpecialEquals(structure);

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
    public void testBuild() {

        Structure structure = createStructure();

        structure.getAttributes().forEach(attribute -> {
            Structure.Attribute buildAttribute = Structure.Attribute.build(attribute);
            assertObjects(Assert::assertEquals, attribute, buildAttribute);
            assertTrue(buildAttribute.storageEquals(attribute));
        });

        structure.getReferences().forEach(reference -> {
            Structure.Reference buildReference = Structure.Reference.build(reference);
            assertObjects(Assert::assertEquals, reference, buildReference);
        });
    }

    @Test
    public void testBuildNull() {
        Structure.Attribute nullAttribute = Structure.Attribute.build(null);
        assertEquals(new Structure.Attribute(), nullAttribute);

        Structure.Reference nullReference = Structure.Reference.build(null);
        assertEquals(new Structure.Reference(), nullReference);
        assertTrue(nullReference.isNull());
    }

    @Test
    public void testBuildWithSet() {

        Structure structure = createStructure();

        structure.getPrimaries().forEach(attribute -> {

            Structure.Attribute createAttribute = Structure.Attribute.build(attribute);
            attribute.setIsPrimary(true);
            assertObjects(Assert::assertEquals, attribute, createAttribute);
            assertTrue(createAttribute.storageEquals(attribute));

            createAttribute = Structure.Attribute.build(attribute);
            attribute.setIsPrimary(null);
            assertObjects(Assert::assertNotEquals, attribute, createAttribute);
            assertTrue(createAttribute.storageEquals(attribute));
        });

        structure.getAttributes().stream().filter(Structure.Attribute::isLocalizable).forEach(attribute -> {

            Structure.Attribute createAttribute = Structure.Attribute.build(attribute);
            attribute.setLocalizable(true);
            assertObjects(Assert::assertEquals, attribute, createAttribute);
            assertTrue(createAttribute.storageEquals(attribute));

            createAttribute = Structure.Attribute.build(attribute);
            attribute.setLocalizable(null);
            assertObjects(Assert::assertNotEquals, attribute, createAttribute);
            assertTrue(createAttribute.storageEquals(attribute));
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
        assertTrue(primary.hasIsPrimary());
        assertFalse(primary.isLocalizable());
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

        Structure.Attribute localizable = localizables.get(0);
        assertTrue(localizable.isLocalizable());
        assertFalse(localizable.hasIsPrimary());
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
            assertFalse(REFERENCE_LIST.get(index).isNull());
        });
    }

    @Test
    public void testGetByRefCode() {

        Structure structure = createStructure();

        assertEmpty(structure.getRefCodeAttributes(null));
        assertEmpty(structure.getRefCodeReferences(null));

        assertEmpty(structure.getRefCodeAttributes(""));
        assertEmpty(structure.getRefCodeReferences(""));

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
    public void testGetCodes() {

        Structure structure = createStructure();
        assertEquals(PRIMARY_CODES,structure.getPrimaryCodes());
        assertEquals(getAllAttributeCodes(),structure.getAttributeCodes());
        assertNotEquals(ATTRIBUTE_CODES,structure.getAttributeCodes());
        assertEquals(REFERENCE_CODES,structure.getReferenceAttributeCodes());
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
    public void testChangeEmptyWithAttribute() {

        Structure oldStructure = new Structure();
        testChangeStructureWithAttribute(oldStructure);
    }

    @Test
    public void testChangeFilledWithAttribute() {

        Structure oldStructure = createStructure();
        testChangeStructureWithAttribute(oldStructure);
    }

    @Test
    public void testChangeEmptyWithReference() {

        Structure oldStructure = new Structure();
        testChangeStructureWithReference(oldStructure);
    }

    @Test
    public void testChangeFilledWithReference() {

        Structure oldStructure = createStructure();
        testChangeStructureWithReference(oldStructure);
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

    private void testChangeStructureWithAttribute(Structure oldStructure) {

        testChangeSimple(oldStructure);
        testChangeNullAttribute(oldStructure);
        testChangeWithAttribute(oldStructure);
    }

    private void testChangeStructureWithReference(Structure oldStructure) {

        testChangeNullReference(oldStructure);
        testChangeWithReference(oldStructure);
    }

    private void testChangeSimple(Structure oldStructure) {

        Structure addedStructure = new Structure(oldStructure);

        Structure.Attribute addingAttribute = copyAttribute(CHANGE_ATTRIBUTE);

        addedStructure.add(addingAttribute, null);
        assertObjects(Assert::assertNotEquals, oldStructure, addedStructure);
        assertFalse(addedStructure.storageEquals(oldStructure));

        Structure.Attribute addedAttribute = addedStructure.getAttribute(CHANGE_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, CHANGE_ATTRIBUTE, addedAttribute);

        Structure updatedStructure = new Structure(addedStructure);

        Structure.Attribute updatingAttribute = copyAttribute(CHANGE_ATTRIBUTE);
        updatingAttribute.setType(FieldType.INTEGER);

        updatedStructure.update(addedAttribute, updatingAttribute);
        assertFalse(updatedStructure.storageEquals(oldStructure));

        Structure.Attribute updatedAttribute = updatedStructure.getAttribute(CHANGE_ATTRIBUTE_CODE);
        assertObjects(Assert::assertNotEquals, CHANGE_ATTRIBUTE, updatedAttribute);
        assertFalse(updatedAttribute.storageEquals(addedAttribute));

        Structure removedStructure = new Structure(updatedStructure);

        removedStructure.remove(CHANGE_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, oldStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(oldStructure));
    }

    private void testChangeNullAttribute(Structure oldStructure) {

        Structure addedStructure = new Structure(oldStructure);

        addedStructure.add(null, null);
        assertObjects(Assert::assertEquals, oldStructure, addedStructure);
        assertTrue(addedStructure.storageEquals(oldStructure));

        assertEquals(oldStructure.getAttributes().size(), addedStructure.getAttributes().size());
        assertEquals(oldStructure.getReferences().size(), addedStructure.getReferences().size());

        addedStructure.add(copyAttribute(CHANGE_ATTRIBUTE), null);
        Structure.Attribute addedAttribute = addedStructure.getAttribute(CHANGE_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, CHANGE_ATTRIBUTE, addedAttribute);

        Structure updatedStructure = new Structure(addedStructure);

        Structure.Attribute nullAttribute = null;
        updatedStructure.update(nullAttribute, null);
        assertObjects(Assert::assertEquals, addedStructure, updatedStructure);

        updatedStructure.update(addedAttribute, null);
        assertObjects(Assert::assertEquals, addedStructure, updatedStructure);

        updatedStructure.update(null, copyAttribute(CHANGE_ATTRIBUTE));
        assertObjects(Assert::assertEquals, addedStructure, updatedStructure);

        Structure removedStructure = new Structure(updatedStructure);

        removedStructure.remove(null);
        assertObjects(Assert::assertEquals, addedStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(addedStructure));

        assertEquals(addedStructure.getAttributes().size(), removedStructure.getAttributes().size());
        assertEquals(addedStructure.getReferences().size(), removedStructure.getReferences().size());
    }

    private void testChangeWithAttribute(Structure oldStructure) {

        Structure addedStructure = new Structure(oldStructure);

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
        assertObjects(Assert::assertNotEquals, CHANGE_ATTRIBUTE, updatingAttribute);

        updatedStructure.update(addedAttribute, updatingAttribute);
        assertFalse(updatedStructure.storageEquals(oldStructure));

        Structure.Attribute updatedAttribute = updatedStructure.getAttribute(CHANGE_ATTRIBUTE_CODE);
        assertEquals(newName, updatedAttribute.getName());
        assertObjects(Assert::assertNotEquals, CHANGE_ATTRIBUTE, updatedAttribute);

        Structure.Reference updatingReference = copyReference(CHANGE_REF_REFERENCE);
        updatingReference.setAttribute(CHANGE_ATTRIBUTE_CODE);
        String newDisplayExpression = updatingReference.getDisplayExpression() + "_updated";
        updatingReference.setDisplayExpression(newDisplayExpression);
        assertObjects(Assert::assertNotEquals, CHANGE_REF_REFERENCE, updatingReference);

        updatedStructure.update(addedReference, updatingReference);
        assertFalse(updatedStructure.storageEquals(oldStructure));

        Structure.Reference updatedReference = updatedStructure.getReference(CHANGE_ATTRIBUTE_CODE);
        assertEquals(newDisplayExpression, updatedReference.getDisplayExpression());
        assertObjects(Assert::assertNotEquals, CHANGE_REF_REFERENCE, updatedReference);

        Structure removedStructure = new Structure(updatedStructure);

        removedStructure.remove(CHANGE_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, oldStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(oldStructure));
    }

    private void testChangeNullReference(Structure oldStructure) {

        Structure addedStructure = new Structure(oldStructure);

        addedStructure.add(null, CHANGE_REF_REFERENCE);
        assertObjects(Assert::assertEquals, oldStructure, addedStructure);
        assertTrue(addedStructure.storageEquals(oldStructure));

        assertEquals(oldStructure.getAttributes().size(), addedStructure.getAttributes().size());
        assertEquals(oldStructure.getReferences().size(), addedStructure.getReferences().size());

        addedStructure.add(copyAttribute(CHANGE_ATTRIBUTE), copyReference(CHANGE_REF_REFERENCE));
        assertEquals(oldStructure.getAttributes().size() + 1, addedStructure.getAttributes().size());
        assertEquals(oldStructure.getReferences().size(), addedStructure.getReferences().size());

        addedStructure.add(copyAttribute(CHANGE_REF_ATTRIBUTE), copyReference(CHANGE_REF_REFERENCE));
        Structure.Reference addedReference = addedStructure.getReference(CHANGE_REF_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, CHANGE_REF_REFERENCE, addedReference);

        Structure updatedStructure = new Structure(addedStructure);
        Structure.Reference nullReference = null;
        updatedStructure.update(nullReference, null);
        assertObjects(Assert::assertEquals, addedStructure, updatedStructure);

        updatedStructure.update(addedReference, null);
        assertEquals(oldStructure.getReferences().size(), updatedStructure.getReferences().size());

        Structure removedStructure = new Structure(addedStructure);

        removedStructure.remove(null);
        assertObjects(Assert::assertEquals, addedStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(addedStructure));

        assertEquals(addedStructure.getAttributes().size(), removedStructure.getAttributes().size());
        assertEquals(addedStructure.getReferences().size(), removedStructure.getReferences().size());
    }

    private void testChangeWithReference(Structure oldStructure) {

        Structure addedStructure = new Structure(oldStructure);

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

        removedStructure.remove(CHANGE_REF_ATTRIBUTE_CODE);
        assertObjects(Assert::assertEquals, oldStructure, removedStructure);
        assertTrue(removedStructure.storageEquals(oldStructure));
    }

    @Test
    public void testGetAttributeCodes() {

        List<String> actual = Structure.getAttributeCodes(ATTRIBUTE_LIST).collect(toList());
        assertEquals(getAllAttributeCodes(), actual);
    }

    @Test
    public void testGetReferenceAttributeCodes() {

        List<String> actual = Structure.getReferenceAttributeCodes(REFERENCE_LIST).collect(toList());
        assertEquals(REFERENCE_CODES, actual);
    }

    /** Создание структуры с глубоким копированием атрибутов и ссылок. */
    private Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
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
}