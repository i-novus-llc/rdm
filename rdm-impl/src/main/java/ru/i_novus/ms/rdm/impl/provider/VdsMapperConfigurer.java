package ru.i_novus.ms.rdm.impl.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.n2oapp.platform.jaxrs.MapperConfigurer;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.diff.DiffFieldValueMixin;
import ru.i_novus.ms.rdm.api.model.diff.DiffRowValueMixin;
import ru.i_novus.ms.rdm.api.model.field.FieldValueMixin;
import ru.i_novus.ms.rdm.api.model.refdata.RowValueMixin;
import ru.i_novus.ms.rdm.impl.model.field.VdsFieldMixin;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

@SuppressWarnings("rawtypes")
public class VdsMapperConfigurer implements MapperConfigurer {

    @Override
    public void configure(ObjectMapper mapper) {

        mapper.addMixIn(RowValue.class, RowValueMixin.class);
        mapper.addMixIn(FieldValue.class, FieldValueMixin.class);
        mapper.addMixIn(DiffRowValue.class, DiffRowValueMixin.class);
        mapper.addMixIn(DiffFieldValue.class, DiffFieldValueMixin.class);
        mapper.addMixIn(Field.class, VdsFieldMixin.class);
        mapper.writerFor(new TypeReference<PageImpl<RowValue>>() {});

        mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // (Де)сериализация даты/времени:
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // в строку / из строки
        mapper.disable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID); // без временно'й зоны

        // (Де)сериализация значений, хранящихся в переменных класса Object:
        mapper.activateDefaultTypingAsProperty(
                BasicPolymorphicTypeValidator.builder().build(),
                ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT,
                "@class");
    }
}
