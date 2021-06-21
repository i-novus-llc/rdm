package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAllowStoreVersionFileStrategyTest {

    @InjectMocks
    private DefaultAllowStoreVersionFileStrategy strategy;

    @Test
    public void testAllow() {

        assertFalse(strategy.allow(null));

        RefBookVersion version = new RefBookVersion();

        version.setStatus(RefBookVersionStatus.DRAFT);
        assertFalse(strategy.allow(version));

        version.setStatus(RefBookVersionStatus.PUBLISHED);
        assertTrue(strategy.allow(version));
    }
}