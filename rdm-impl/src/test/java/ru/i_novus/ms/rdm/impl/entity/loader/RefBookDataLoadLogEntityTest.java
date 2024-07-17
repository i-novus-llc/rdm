package ru.i_novus.ms.rdm.impl.entity.loader;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.impl.BaseTest;

public class RefBookDataLoadLogEntityTest extends BaseTest {

    @Test
    public void testClass() {

        final RefBookDataLoadLogEntity entity = new RefBookDataLoadLogEntity();
        assertSpecialEquals(entity);

        final RefBookDataLoadLogEntity logEntity = createRefBookDataLoadLogEntity();
        assertObjects(Assert::assertNotEquals, entity, logEntity);

        RefBookDataLoadLogEntity copyEntity = new RefBookDataLoadLogEntity();
        copyEntity.setId(logEntity.getId());
        assertObjects(Assert::assertNotEquals, logEntity, copyEntity);
        copyEntity.setChangeSetId(logEntity.getChangeSetId());
        assertObjects(Assert::assertNotEquals, logEntity, copyEntity);
        copyEntity.setCode(logEntity.getCode());
        assertObjects(Assert::assertEquals, logEntity, copyEntity);
    }

    private static RefBookDataLoadLogEntity createRefBookDataLoadLogEntity() {

        final RefBookDataLoadLogEntity logEntity = new RefBookDataLoadLogEntity();
        logEntity.setId(1);
        logEntity.setChangeSetId("change_set_id");
        logEntity.setCode("ref_book_code");

        return logEntity;
    }
}