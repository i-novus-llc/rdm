package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedAllowStoreVersionFileStrategyTest {

    @InjectMocks
    private UnversionedAllowStoreVersionFileStrategy strategy;

    @Test
    public void testAllow() {

        assertFalse(strategy.allow(null));

        RefBookVersion version = new RefBookVersion();

        version.setType(RefBookTypeEnum.UNVERSIONED);
        assertFalse(strategy.allow(version));
    }
}