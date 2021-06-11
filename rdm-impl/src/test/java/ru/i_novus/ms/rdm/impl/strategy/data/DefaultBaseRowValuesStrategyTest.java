package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
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
public abstract class DefaultBaseRowValuesStrategyTest {

    protected static final int REFBOOK_ID = 1;
    protected static final String REFBOOK_CODE = "test";

    protected static final int DRAFT_ID = 2;
    protected static final String DRAFT_CODE = "draft_code";

    protected static final String NAME_FIELD_VALUE_PREFIX = "name_";
    protected static final String TEXT_FIELD_VALUE_PREFIX = "text with id = ";

    protected RefBookVersionEntity createDraftEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setRefBook(createRefBookEntity());
        entity.setStructure(createStructure());
        entity.setStorageCode(DRAFT_CODE);
        entity.setStatus(RefBookVersionStatus.DRAFT);

        return entity;
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
