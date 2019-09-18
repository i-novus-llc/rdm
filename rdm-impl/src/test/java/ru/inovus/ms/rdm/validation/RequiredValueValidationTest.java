package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;
import ru.inovus.ms.rdm.n2o.model.Structure;

import java.util.*;

import static java.util.Collections.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RequiredValueValidationTest {

    private final String PRIMARY = "primary";
    private final String PRIMARY_NAME = "primary";
    private final String IGNORED_PRIMARY = "ignore";
    private final String IGNORED_PRIMARY_NAME = "ignore";
    private final String NON_REQUIRED = "nonRequired";
    private final String NON_REQUIRED_NAME = "nonRequired";

    private Structure structure;

    private Row nullRow;
    private Row fullRow;

    @Before
    public void setUp() throws Exception {
        Structure.Attribute id = Structure.Attribute.buildPrimary(PRIMARY, PRIMARY_NAME, FieldType.INTEGER, "");
        Structure.Attribute ignoredReq = Structure.Attribute.buildPrimary(IGNORED_PRIMARY, IGNORED_PRIMARY_NAME, FieldType.REFERENCE, "");
        Structure.Attribute nonReq = Structure.Attribute.build(NON_REQUIRED, NON_REQUIRED_NAME, FieldType.FLOAT, "");
        structure = new Structure(Arrays.asList(id, ignoredReq, nonReq), null);

        nullRow = new Row(new HashMap<>());
        Map<String, Object> fullRowMap = new HashMap<>();
        fullRowMap.put(PRIMARY, "test Value");
        fullRowMap.put(IGNORED_PRIMARY, "test Value");
        fullRowMap.put(NON_REQUIRED, "test Value");
        fullRow = new Row(fullRowMap);

    }

    @Test
    public void testValidate() throws Exception {
        List<Message> messages = new PkRequiredValidation(nullRow, structure, singleton(IGNORED_PRIMARY)).validate();
        assertEquals(1, messages.size());
        Message expected1_1 = new Message(PkRequiredValidation.REQUIRED_ERROR_CODE, PRIMARY);
        assertTrue(messages.contains(expected1_1));

        List<Message> messages2 = new PkRequiredValidation(nullRow, structure, emptySet()).validate();
        assertEquals(2, messages2.size());
        Message expected2_1 = new Message(PkRequiredValidation.REQUIRED_ERROR_CODE, PRIMARY);
        assertTrue(messages2.contains(expected2_1));
        Message expected2_3 = new Message(PkRequiredValidation.REQUIRED_ERROR_CODE, IGNORED_PRIMARY);
        assertTrue(messages2.contains(expected2_3));

        List<Message> messages3 = new PkRequiredValidation(fullRow, structure, emptySet()).validate();
        assertEquals(0, messages3.size());
    }
}
