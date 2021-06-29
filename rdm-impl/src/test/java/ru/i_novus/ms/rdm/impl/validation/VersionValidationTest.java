package ru.i_novus.ms.rdm.impl.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.impl.BaseTest;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("java:S5778")
public class VersionValidationTest extends BaseTest {

    private static final Integer REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";
    private static final Integer VERSION_ID = 2;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private VersionValidationImpl versionValidation;

    @Mock
    private RefBookRepository refBookRepository;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testValidateRefBook() {

        when(refBookRepository.existsById(REFBOOK_ID)).thenReturn(true);
        when(versionRepository.exists(any(BooleanExpression.class))).thenReturn(false);
        try {
            versionValidation.validateRefBook(REFBOOK_ID);

        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateRefBookCode() {
        try {
            versionValidation.validateRefBookCode("aBook-0");

        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateRefBookCodeFail() {
        try {
            versionValidation.validateRefBookCode("Реестр");
            fail(getFailedMessage(UserException.class));

        } catch (UserException e) {
            assertNotNull(e.getMessages());
            assertEquals(2, e.getMessages().size());
            assertEquals("refbook.code.is.invalid", e.getMessages().get(0).getCode());
            assertEquals("code.is.invalid", e.getMessages().get(1).getCode());
        }
    }

    @Test
    public void testValidateRefBookExists() {

        when(refBookRepository.existsById(REFBOOK_ID)).thenReturn(true);
        try {
            versionValidation.validateRefBookExists(REFBOOK_ID);

        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateRefBookCodeExists() {

        when(refBookRepository.existsByCode(REFBOOK_CODE)).thenReturn(true);
        try {
            versionValidation.validateRefBookCodeExists(REFBOOK_CODE);

        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateRefBookCodeExistsFail() {

        when(refBookRepository.existsByCode(REFBOOK_CODE)).thenReturn(false);
        try {
            versionValidation.validateRefBookCodeExists(REFBOOK_CODE);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals("refbook.with.code.not.found", getExceptionMessage(e));
        }
    }

    @Test
    public void testValidateRefBookCodeNotExists() {

        when(refBookRepository.existsByCode(REFBOOK_CODE)).thenReturn(false);
        try {
            versionValidation.validateRefBookCodeNotExists(REFBOOK_CODE);

        } catch (RuntimeException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateRefBookCodeNotExistsFail() {

        when(refBookRepository.existsByCode(REFBOOK_CODE)).thenReturn(true);
        try {
            versionValidation.validateRefBookCodeNotExists(REFBOOK_CODE);
            fail(getFailedMessage(UserException.class));

        } catch (RuntimeException e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("refbook.with.code.already.exists", getExceptionMessage(e));
        }
    }
}