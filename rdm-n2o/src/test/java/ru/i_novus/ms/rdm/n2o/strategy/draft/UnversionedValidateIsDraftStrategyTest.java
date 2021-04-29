package ru.i_novus.ms.rdm.n2o.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedValidateIsDraftStrategyTest {

    @InjectMocks
    private UnversionedValidateIsDraftStrategy strategy;

    @Test
    public void validate() {

        RefBookVersion version = new RefBookVersion();
        version.setType(RefBookTypeEnum.UNVERSIONED);

        try {
            strategy.validate(version);

        } catch (Exception e) {
            fail("Unexpected exception throws");
        }
    }
}