package ru.i_novus.ms.rdm.impl.entity.diff;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.impl.BaseTest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

public class RefBookVersionDiffEntityTest extends BaseTest {

    @Test
    public void testClass() {

        RefBookVersionDiffEntity entity = new RefBookVersionDiffEntity();
        assertSpecialEquals(entity);

        RefBookVersionDiffEntity versionDiffEntity = new RefBookVersionDiffEntity(
                createVersionEntity(1), createVersionEntity(2)
        );
        versionDiffEntity.setId(10);
        assertObjects(Assert::assertNotEquals, entity, versionDiffEntity);

        RefBookVersionDiffEntity copyEntity = new RefBookVersionDiffEntity();
        copyEntity.setId(versionDiffEntity.getId());
        assertObjects(Assert::assertNotEquals, versionDiffEntity, copyEntity);
        copyEntity.setOldVersion(versionDiffEntity.getOldVersion());
        assertObjects(Assert::assertNotEquals, versionDiffEntity, copyEntity);
        copyEntity.setNewVersion(versionDiffEntity.getNewVersion());
        assertObjects(Assert::assertEquals, versionDiffEntity, copyEntity);
    }

    private RefBookVersionEntity createVersionEntity(Integer id) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(id);

        return entity;
    }
}