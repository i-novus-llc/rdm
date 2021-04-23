package ru.i_novus.ms.rdm.n2o.strategy.draft;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class DefaultValidateIsDraftStrategyTest {

    private static final int REFBOOK_ID = 1;
    private static final String REFBOOK_CODE = "TEST_CODE";

    private static final int VERSION_ID = 2;

    @InjectMocks
    private DefaultValidateIsDraftStrategy strategy;

    @Test
    public void testValidateDraft() {

        RefBookVersion version = createRefBookVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);

        try {
            strategy.validate(version);

        } catch (Exception e) {
            fail("Unexpected exception throws");
        }
    }

    @Test
    public void testValidateVersion() {

        RefBookVersion version = createRefBookVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);

        try {
            strategy.validate(version);
            fail(String.format("Exception expected with message: %s", "version.is.not.draft"));

        } catch (Exception e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("version.is.not.draft", e.getMessage());
        }
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setRefBookId(REFBOOK_ID);
        result.setCode(REFBOOK_CODE);

        return result;
    }
}