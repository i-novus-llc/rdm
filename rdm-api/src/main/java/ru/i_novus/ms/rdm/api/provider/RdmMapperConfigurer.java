package ru.i_novus.ms.rdm.api.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.MapperConfigurer;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.diff.DiffFieldValueMixin;
import ru.i_novus.ms.rdm.api.model.diff.DiffRowValueMixin;
import ru.i_novus.ms.rdm.api.model.field.FieldMixin;
import ru.i_novus.ms.rdm.api.model.field.FieldValueMixin;
import ru.i_novus.ms.rdm.api.model.refdata.RowValueMixin;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

@SuppressWarnings("rawtypes")
public class RdmMapperConfigurer implements MapperConfigurer {

    @Override
    public void configure(ObjectMapper mapper) {

        mapper.addMixIn(RowValue.class, RowValueMixin.class);
        mapper.addMixIn(FieldValue.class, FieldValueMixin.class);
        mapper.addMixIn(DiffRowValue.class, DiffRowValueMixin.class);
        mapper.addMixIn(DiffFieldValue.class, DiffFieldValueMixin.class);
        mapper.addMixIn(Field.class, FieldMixin.class);
        mapper.writerFor(new TypeReference<PageImpl<RowValue>>() {});

        mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
