package ru.inovus.ms.rdm.file;

import net.n2oapp.platform.i18n.Message;
import org.junit.Assert;
import org.junit.Test;
import ru.inovus.ms.rdm.validation.TypeValidation;

import java.util.*;

import static ru.inovus.ms.rdm.file.BufferedRowsPersisterTest.createTestStructure;

public class TypeValidationTest {

    @Test
    public void testValidate(){
        Map<String, Object> row = new LinkedHashMap<>() {{
            put("name", "name");
            put("count", "wrong type");
        }};
        List<Message> expected = Collections.singletonList(new Message("validation.type.error", "count",  "wrong type"));

        TypeValidation typeValidation = new TypeValidation(row, createTestStructure());
        List<Message> actual = typeValidation.validate();
        Set<String> errorAttributes = typeValidation.getErrorAttributes();

        Assert.assertEquals(expected, actual);
        Assert.assertEquals(1, errorAttributes.size());
        Assert.assertTrue(errorAttributes.contains("count"));
    }


}
