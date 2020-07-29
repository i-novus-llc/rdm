package ru.i_novus.ms.rdm.api.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.MapperConfigurer;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DiffRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.ms.rdm.api.model.diff.DiffRowValueMixin;
import ru.i_novus.ms.rdm.api.model.field.FieldMixin;
import ru.i_novus.ms.rdm.api.model.field.FieldValueMixin;
import ru.i_novus.ms.rdm.api.model.refdata.RowValueMixin;

public class RdmMapperConfigurer implements MapperConfigurer {

    @Override
    public void configure(ObjectMapper mapper) {
        mapper.addMixIn(RowValue.class, RowValueMixin.class);
        mapper.addMixIn(FieldValue.class, FieldValueMixin.class);
        mapper.addMixIn(DiffRowValue.class, DiffRowValueMixin.class);
        mapper.addMixIn(Field.class, FieldMixin.class);
        mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.writerFor(new TypeReference<PageImpl<RowValue>>() {});
    }

}
