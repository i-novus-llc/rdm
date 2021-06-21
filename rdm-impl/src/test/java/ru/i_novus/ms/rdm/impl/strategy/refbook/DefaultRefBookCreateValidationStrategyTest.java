package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRefBookCreateValidationStrategyTest {

    @InjectMocks
    private DefaultRefBookCreateValidationStrategy strategy;

    @Mock
    private VersionValidation versionValidation;

    @Test
    public void testValidate() {

        final String testCode = "test_code";

        strategy.validate(testCode);

        verify(versionValidation).validateRefBookCode(eq(testCode));
        verify(versionValidation).validateRefBookCodeNotExists(eq(testCode));

        verifyNoMoreInteractions(versionValidation);
    }
}