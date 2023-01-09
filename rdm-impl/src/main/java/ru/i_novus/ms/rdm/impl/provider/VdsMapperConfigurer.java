package ru.i_novus.ms.rdm.impl.provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.n2oapp.platform.jaxrs.MapperConfigurer;
import ru.i_novus.ms.rdm.api.provider.DataMapperConfigurer;
import ru.i_novus.ms.rdm.impl.model.field.VdsFieldMixin;
import ru.i_novus.platform.datastorage.temporal.model.Field;

public class VdsMapperConfigurer extends DataMapperConfigurer implements MapperConfigurer {

    @Override
    public void configure(ObjectMapper mapper) {

        mapper.addMixIn(Field.class, VdsFieldMixin.class);

        super.configure(mapper);

        mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // (Де)сериализация даты/времени:
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // в строку / из строки
        mapper.disable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID); // без временно'й зоны

        // (Де)сериализация значений, хранящихся в полях класса Object:
        //mapper.activateDefaultTypingAsProperty(
        //        BasicPolymorphicTypeValidator.builder().build(),
        //        ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT,
        //        "@class");

        // Пропуск значений null:
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
