package ru.i_novus.ms.rdm.impl.model.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.impl.service.diff.DataDiffUtil;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FieldMixinTest {

    private static final ObjectMapper vdsObjectMapper = DataDiffUtil.getVdsObjectMapper();

    @Test
    public void testJson() {

        testJson(IntegerField.class);
        testJson(FloatField.class);
        testJson(StringField.class);
        testJson(DateField.class);
        testJson(BooleanField.class);
        testJson(ReferenceField.class);
        testJson(IntegerStringField.class);
    }

    private <T> void testJson(Class<T> clazz) {

        T field = createField(clazz);
        if (field == null)
            return;

        String json = JsonUtil.toJsonString(vdsObjectMapper, field);
        T restored = JsonUtil.fromJsonString(vdsObjectMapper, json, clazz);
        assertEquals(field.getClass(), restored.getClass());
        assertEquals(field, restored);
    }

    private <T> T createField(Class<T> clazz) {
        try {
            return (T) clazz.getConstructor(String.class).newInstance("text");

        } catch (ReflectiveOperationException e) {
            fail();
            return null;
        }
    }
}
