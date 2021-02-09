package ru.i_novus.ms.rdm.api.util;

import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.Structure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StructureUtilsTest {

    @Test
    public void testIsReference() {

        assertFalse(StructureUtils.isReference(null));

        Structure.Reference nullReference = new Structure.Reference();
        assertFalse(StructureUtils.isReference(nullReference));

        Structure.Reference badReference = new Structure.Reference("attr", null, null);
        assertFalse(StructureUtils.isReference(badReference));

        Structure.Reference partialReference = new Structure.Reference("attr", "ref", null);
        assertTrue(StructureUtils.isReference(partialReference));

        Structure.Reference fullReference = new Structure.Reference("attr", "ref", "${code}");
        assertTrue(StructureUtils.isReference(fullReference));
    }
}