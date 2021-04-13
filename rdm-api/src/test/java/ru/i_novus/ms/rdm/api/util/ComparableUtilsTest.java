package ru.i_novus.ms.rdm.api.util;

import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableField;
import ru.i_novus.ms.rdm.api.model.diff.RefBookAttributeDiff;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.i_novus.ms.rdm.api.util.ComparableUtils.createCommonComparableFields;

public class ComparableUtilsTest extends BaseTest {

    private static final String PRIMARY = "primary";
    private static final String DELETED1 = "deleted1";
    private static final String DELETED2 = "deleted2";
    private static final String INSERTED1 = "inserted1";
    private static final String INSERTED2 = "inserted2";
    private static final String UPDATED = "updated";

    @Test
    public void testCreateCommonComparableFields() {
        
        Structure.Attribute primary = Structure.Attribute.buildPrimary(PRIMARY, PRIMARY, FieldType.INTEGER, null);
        Structure.Attribute deleted1 = Structure.Attribute.build(DELETED1, DELETED1, FieldType.STRING, null);
        Structure.Attribute deleted2 = Structure.Attribute.build(DELETED2, DELETED2, FieldType.BOOLEAN, null);
        Structure.Attribute inserted1 = Structure.Attribute.build(INSERTED1, INSERTED1, FieldType.STRING, null);
        Structure.Attribute inserted2 = Structure.Attribute.build(INSERTED2, INSERTED2, FieldType.INTEGER, null);

        Structure.Attribute oldUpdated = Structure.Attribute.build(UPDATED, UPDATED, FieldType.INTEGER, null);
        Structure.Attribute newUpdated = Structure.Attribute.build(UPDATED, UPDATED, FieldType.STRING, null);

        Structure oldStructure = new Structure(List.of(primary, deleted1, oldUpdated, deleted2), null);
        Structure newStructure = new Structure(List.of(primary, inserted1, newUpdated, inserted2), null);

        List<ComparableField> expected = List.of(
                new ComparableField(PRIMARY, PRIMARY, null),
                new ComparableField(INSERTED1, INSERTED1, DiffStatusEnum.INSERTED),
                new ComparableField(UPDATED, UPDATED, DiffStatusEnum.UPDATED),
                new ComparableField(INSERTED2, INSERTED2, DiffStatusEnum.INSERTED),
                new ComparableField(DELETED1, DELETED1, DiffStatusEnum.DELETED),
                new ComparableField(DELETED2, DELETED2, DiffStatusEnum.DELETED)
        );

        RefBookAttributeDiff attributeDiff = new RefBookAttributeDiff(List.of(DELETED1, DELETED2),
                List.of(INSERTED1, INSERTED2), singletonList(UPDATED));
        List<ComparableField> actual = createCommonComparableFields(attributeDiff, newStructure, oldStructure);
        assertNotNull(actual);
        assertEquals(1+1+2+2, actual.size());
        assertEquals(expected, actual);
    }
}