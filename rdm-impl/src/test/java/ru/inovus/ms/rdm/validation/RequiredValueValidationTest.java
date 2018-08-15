package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.file.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class RequiredValueValidationTest {

    private final String PRIMARY = "primary";
    private final String PRIMARY_NAME = "primary";
    private final String IGNORED_REQUIRED = "ignore";
    private final String IGNORED_REQUIRED_NAME = "ignore";
    private final String REQUIRED = "required";
    private final String REQUIRED_NAME = "required";
    private final String NON_REQUIRED = "nonRequired";
    private final String NON_REQUIRED_NAME = "nonRequired";

    private Structure structure;

    private Row nullRow;
    private Row fullRow;

    @Before
    public void setUp() throws Exception {
        Structure.Attribute id = Structure.Attribute.buildPrimary(PRIMARY, PRIMARY_NAME, FieldType.INTEGER, "");
        Structure.Attribute ignoredReq = Structure.Attribute.build(IGNORED_REQUIRED, IGNORED_REQUIRED_NAME, FieldType.REFERENCE, true, "");
        Structure.Attribute req = Structure.Attribute.build(REQUIRED, REQUIRED_NAME, FieldType.STRING, true, "");
        Structure.Attribute nonReq = Structure.Attribute.build(NON_REQUIRED, NON_REQUIRED_NAME, FieldType.FLOAT, false, "");
        structure = new Structure(Arrays.asList(id, ignoredReq, req, nonReq), null);

        nullRow = new Row(new HashMap<>());
        Map<String, Object> fullRowMap = new HashMap<>();
        fullRowMap.put(PRIMARY, "test Value");
        fullRowMap.put(REQUIRED, "test Value");
        fullRowMap.put(IGNORED_REQUIRED, "test Value");
        fullRowMap.put(NON_REQUIRED, "test Value");
        fullRow = new Row(fullRowMap);

    }

    @Test
    public void testValidate() throws Exception {
        List<Message> messages = new RequiredValidation(nullRow, structure, Collections.singleton(IGNORED_REQUIRED)).validate();
        Assert.assertTrue(messages.size() == 2);
        Message expected1_1 = new Message(RequiredValidation.ERROR_CODE, PRIMARY);
        Assert.assertTrue(messages.contains(expected1_1));
        Message expected1_2 = new Message(RequiredValidation.ERROR_CODE, REQUIRED);
        Assert.assertTrue(messages.contains(expected1_2));

        List<Message> messages2 = new RequiredValidation(nullRow, structure, Collections.EMPTY_SET).validate();
        Assert.assertTrue(messages2.size() == 3);
        Message expected2_1 = new Message(RequiredValidation.ERROR_CODE, PRIMARY);
        Assert.assertTrue(messages2.contains(expected2_1));
        Message expected2_2 = new Message(RequiredValidation.ERROR_CODE, REQUIRED);
        Assert.assertTrue(messages2.contains(expected2_2));
        Message expected2_3 = new Message(RequiredValidation.ERROR_CODE, IGNORED_REQUIRED);
        Assert.assertTrue(messages2.contains(expected2_3));

        List<Message> messages3 = new RequiredValidation(fullRow, structure, Collections.EMPTY_SET).validate();
        Assert.assertTrue(messages3.size() == 0);
    }
}
