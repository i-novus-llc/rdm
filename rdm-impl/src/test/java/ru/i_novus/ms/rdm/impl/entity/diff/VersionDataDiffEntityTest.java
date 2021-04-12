package ru.i_novus.ms.rdm.impl.entity.diff;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.impl.BaseTest;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

public class VersionDataDiffEntityTest extends BaseTest {

    @Test
    public void testClass() {

        VersionDataDiffEntity entity = new VersionDataDiffEntity();
        assertSpecialEquals(entity);

        VersionDataDiffEntity diffEntity = new VersionDataDiffEntity();
        diffEntity.setId(100);
        diffEntity.setVersionDiffEntity(createVersionDiffEntity());
        diffEntity.setPrimaries("1,'2'");
        diffEntity.setValues("{a: 1; b: \"2\"; c: \"2021-02-02\"}");
        assertObjects(Assert::assertNotEquals, entity, diffEntity);

        VersionDataDiffEntity copyEntity = new VersionDataDiffEntity();
        copyEntity.setId(diffEntity.getId());
        assertObjects(Assert::assertNotEquals, diffEntity, copyEntity);
        copyEntity.setVersionDiffEntity(diffEntity.getVersionDiffEntity());
        assertObjects(Assert::assertNotEquals, diffEntity, copyEntity);
        copyEntity.setPrimaries(diffEntity.getPrimaries());
        assertObjects(Assert::assertNotEquals, diffEntity, copyEntity);
        copyEntity.setValues(diffEntity.getValues());
        assertObjects(Assert::assertEquals, diffEntity, copyEntity);
    }

    private RefBookVersionDiffEntity createVersionDiffEntity() {

        return new RefBookVersionDiffEntity(
                createVersionEntity(1), createVersionEntity(2)
        );
    }

    private RefBookVersionEntity createVersionEntity(Integer id) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(id);

        return entity;
    }
}