package ru.i_novus.ms.rdm.api.provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.n2oapp.platform.jaxrs.MapperConfigurer;
import ru.i_novus.ms.rdm.api.model.field.FieldMixin;
import ru.i_novus.platform.datastorage.temporal.model.Field;

public class RdmMapperConfigurer extends DataMapperConfigurer implements MapperConfigurer {

    @Override
    public void configure(ObjectMapper mapper) {

        mapper.addMixIn(Field.class, FieldMixin.class); // CommonField used only

        super.configure(mapper);

        mapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
