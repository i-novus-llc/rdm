package ru.i_novus.ms.rdm.impl.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;

import static java.util.Arrays.asList;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.*;

@RunWith(MockitoJUnitRunner.class)
public abstract class DefaultBaseStrategyTest {

    protected static final int REFBOOK_ID = 1;
    protected static final String REFBOOK_CODE = "test";

    protected static final int DRAFT_ID = 2;
    protected static final String DRAFT_CODE = "draft_code";
    protected static final int DRAFT_OPT_LOCK_VALUE = 10;

    protected static final String NAME_FIELD_VALUE_PREFIX = "name_";
    protected static final String TEXT_FIELD_VALUE_PREFIX = "text with id = ";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    protected RefBookVersionEntity createDraftEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setRefBook(createRefBookEntity());
        entity.setStructure(createStructure());
        entity.setStorageCode(DRAFT_CODE);
        entity.setStatus(RefBookVersionStatus.DRAFT);

        return entity;
    }

    protected void fillOptLockValue(RefBookVersionEntity entity, int optLockValue) {
        try {
            FieldSetter.setField(entity, RefBookVersionEntity.class.getDeclaredField("optLockValue"), optLockValue);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    protected RefBookEntity createRefBookEntity() {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFBOOK_ID);
        entity.setCode(REFBOOK_CODE);

        return entity;
    }

    protected Structure createStructure() {

        return new Structure(DEFAULT_STRUCTURE);
    }

    protected LongRowValue createRowValue(Long systemId, Integer id) {

        return createRowValue(systemId, BigInteger.valueOf(id), NAME_FIELD_VALUE_PREFIX + id, TEXT_FIELD_VALUE_PREFIX + id);
    }

    protected LongRowValue createRowValue(Long systemId, BigInteger id, String name, String text) {

        return new LongRowValue(systemId, asList(
                new IntegerFieldValue(ID_ATTRIBUTE_CODE, id),
                new StringFieldValue(NAME_ATTRIBUTE_CODE, name),
                new StringFieldValue(STRING_ATTRIBUTE_CODE, text)
        ));
    }
}
