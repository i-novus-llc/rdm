package ru.i_novus.ms.rdm.n2o.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedFindOrCreateDraftStrategyTest {

    private static final int REFBOOK_ID = 1;
    private static final String REFBOOK_CODE = "TEST_CODE";

    private static final int VERSION_ID = 2;

    @InjectMocks
    private UnversionedFindOrCreateDraftStrategy strategy;

    @Test
    public void testFindOrCreate() {

        RefBookVersion version = createRefBookVersion();

        UiDraft expected = new UiDraft(version);

        UiDraft actual = strategy.findOrCreate(version);
        assertEquals(expected, actual);
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setRefBookId(REFBOOK_ID);
        result.setCode(REFBOOK_CODE);
        result.setType(RefBookTypeEnum.UNVERSIONED);

        return result;
    }
}