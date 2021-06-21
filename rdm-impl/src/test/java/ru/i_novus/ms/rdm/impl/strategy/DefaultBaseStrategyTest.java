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
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.i_novus.platform.datastorage.temporal.model.value.StringFieldValue;

import java.math.BigInteger;
import java.util.List;

import static java.util.Arrays.asList;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.*;

@RunWith(MockitoJUnitRunner.class)
public abstract class DefaultBaseStrategyTest {

    protected static final int REFBOOK_ID = 1;
    protected static final String REFBOOK_CODE = "test";

    protected static final int DRAFT_ID = 2;
    protected static final String DRAFT_CODE = "draft_code";
    protected static final int DRAFT_OPT_LOCK_VALUE = 10;

    protected static final int REFERRED_ID = 20;
    protected static final int REFERRED_VERSION_ID = 22;

    protected static final Structure.Attribute REFERRED_ATTRIBUTE = Structure.Attribute.build(
            REFERRED_BOOK_ATTRIBUTE_CODE, REFERRED_BOOK_ATTRIBUTE_CODE.toLowerCase(), FieldType.STRING, null
    );
    protected static final Structure REFERRED_STRUCTURE = new Structure(List.of(ID_ATTRIBUTE, REFERRED_ATTRIBUTE), null);

    protected static final String NAME_FIELD_VALUE_PREFIX = "name_";
    protected static final String TEXT_FIELD_VALUE_PREFIX = "text with id = ";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    protected RefBookVersionEntity createDraftEntity() {

        RefBookEntity refBookEntity = createDefaultRefBookEntity();
        RefBookVersionEntity entity = refBookEntity.createChangeableVersion();

        entity.setId(DRAFT_ID);
        entity.setRefBook(createDefaultRefBookEntity());
        entity.setStructure(createStructure());
        entity.setStorageCode(DRAFT_CODE);

        return entity;
    }

    protected RefBookVersionEntity createReferredVersionEntity(Structure.Reference reference) {

        RefBookEntity refBookEntity = new DefaultRefBookEntity();
        refBookEntity.setId(REFERRED_ID);
        refBookEntity.setCode(reference.getReferenceCode());

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(REFERRED_VERSION_ID);
        entity.setRefBook(refBookEntity);
        entity.setStructure(new Structure(REFERRED_STRUCTURE));
        entity.setStatus(RefBookVersionStatus.PUBLISHED);

        return entity;
    }

    protected void fillOptLockValue(RefBookVersionEntity entity, int optLockValue) {
        try {
            FieldSetter.setField(entity, RefBookVersionEntity.class.getDeclaredField("optLockValue"), optLockValue);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    protected RefBookEntity createDefaultRefBookEntity() {

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
