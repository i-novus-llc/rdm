package ru.i_novus.ms.rdm.rest.loader;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataResponse;
import ru.i_novus.ms.rdm.api.service.loader.RefBookDataLoaderService;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum.CREATE_ONLY;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"rawtypes","java:S5778"})
public class RefBookDataServerLoaderTest extends BaseLoaderTest {

    private static final String LOADED_SUBJECT = "test";
    private static final String LOADED_TARGET = "refBookData";

    @InjectMocks
    private RefBookDataServerLoader loader;

    @Mock
    private RefBookDataLoaderService service;

    @Test
    public void testGetTarget() {

        final String actual = loader.getTarget();
        assertEquals(LOADED_TARGET, actual);
    }

    @Test
    public void testGetDataType() {

        final Class expected = RefBookDataRequest.class;
        final Class actual = loader.getDataType();

        assertEquals(expected, actual);
    }

    @Test
    public void testLoad() {

        final RefBookDataRequest request = createFileDataRequest(REFBOOK_ID, CREATE_ONLY);
        final RefBookDataResponse response = new RefBookDataResponse(REFBOOK_ID, null);
        when(service.load(request)).thenReturn(response);

        loader.load(List.of(request), LOADED_SUBJECT);

        verify(service, times(1)).load(request);

        verifyNoMoreInteractions(service);
    }

    @Test
    public void testLoadWhenEmptyData() {

        loader.load(null, LOADED_SUBJECT);

        verify(service, times(0)).load(any(RefBookDataRequest.class));
    }

    @Test
    public void testLoadWhenFileModelFailedOther() {

        final RefBookDataRequest request = createFileDataRequest(REFBOOK_ID, CREATE_ONLY);

        final String errorCode = "refbook.with.code.other.error";
        when(service.load(request)).thenThrow(new UserException(errorCode));

        try {
            loader.load(List.of(request), LOADED_SUBJECT);
            fail(getFailedMessage(UserException.class));

        } catch (UserException e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals(errorCode, getExceptionMessage(e));
        }
    }

    @Test
    public void testLoadWhenFileModelFailedRethrowed() {

        final RefBookDataRequest request = createFileDataRequest(REFBOOK_ID, CREATE_ONLY);

        final String errorCode = "some error is rethrowed";
        when(service.load(request)).thenThrow(new NotFoundException(errorCode));

        try {
            loader.load(List.of(request), LOADED_SUBJECT);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals(errorCode, getExceptionMessage(e));
        }
    }
}