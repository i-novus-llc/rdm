package ru.i_novus.ms.rdm.impl.entity.diff;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.test.BaseTest;

public class RefBookVersionDiffEntityTest extends BaseTest {

    @Test
    public void testClass() {

        RefBookVersionDiffEntity entity = new RefBookVersionDiffEntity();
        assertSpecialEquals(entity);

        RefBookVersionDiffEntity versionDiffEntity = new RefBookVersionDiffEntity(
                createVersionEntity(1), createVersionEntity(2)
        );
        assertObjects(Assert::assertNotEquals, entity, versionDiffEntity);

        RefBookVersionDiffEntity copyEntity = new RefBookVersionDiffEntity();
        copyEntity.setOldVersion(versionDiffEntity.getOldVersion());
        copyEntity.setNewVersion(versionDiffEntity.getNewVersion());
        assertObjects(Assert::assertEquals, versionDiffEntity, copyEntity);
    }

    private RefBookVersionEntity createVersionEntity(Integer id) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(id);

        return entity;
    }
}