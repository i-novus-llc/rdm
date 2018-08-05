package ru.inovus.ms.rdm.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.MapperConfigurer;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.inovus.ms.rdm.model.FieldValueMixin;
import ru.inovus.ms.rdm.model.RowValueMixin;

public class RowValueMapperPreparer implements MapperConfigurer {

    @Override
    public void configure (ObjectMapper mapper) {
        mapper.addMixIn(RowValue.class, RowValueMixin.class);
        mapper.addMixIn(FieldValue.class, FieldValueMixin.class);
        mapper.writerFor(new TypeReference<PageImpl<RowValue>>() {
        });

    }
}
