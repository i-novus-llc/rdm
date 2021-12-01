package ru.i_novus.ms.rdm.rest.loader;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.rest.BaseTest;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({"rawtypes","java:S5778"})
public class RefBookDataServerLoaderTest extends BaseTest {

    private static final String LOADED_SUBJECT = "test";
    private static final String LOADED_TARGET = "refBookData";

    private static final String LOADED_CODE = "LOADED_DATA_";

    @InjectMocks
    private RefBookDataServerLoader loader;

    @Mock
    private RefBookService refBookService;

    @Mock
    private PublishService publishService;

    @Test
    public void testGetTarget() {

        String actual = loader.getTarget();
        assertEquals(LOADED_TARGET, actual);
    }

    @Test
    public void testGetDataType() {

        Class expected = RefBookDataRequest.class;
        Class actual = loader.getDataType();

        assertEquals(expected, actual);
    }

    @Test
    public void testLoad() {

        RefBookDataRequest request = createRequest();

        Draft draft = new Draft(10, "storage-code", 0);
        when(refBookService.create(eq(request.getFileModel()))).thenReturn(draft);

        loader.load(List.of(request), LOADED_SUBJECT);

        verify(refBookService, times(1)).create(eq(request.getFileModel()));

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(publishService, times(1)).publish(eq(draft.getId()), captor.capture());

        PublishRequest expectedPublishRequest = new PublishRequest();

        PublishRequest actualPublishRequest = captor.getValue();
        assertNotNull(actualPublishRequest);

        assertEquals(expectedPublishRequest.toString(), actualPublishRequest.toString());
    }

    @Test
    public void testLoadWhenEmptyData() {

        loader.load(null, LOADED_SUBJECT);

        verify(refBookService, times(0)).create(any(FileModel.class));
    }

    @Test
    public void testLoadWhenEmptyFileModel() {

        RefBookDataRequest request = new RefBookDataRequest();
        loader.load(List.of(request), LOADED_SUBJECT);

        verify(refBookService, times(0)).create(any(FileModel.class));
    }

    @Test
    public void testLoadWhenFileModelFailedExisted() {

        RefBookDataRequest request = createRequest();

        final String errorCode = "refbook.with.code.already.exists";
        when(refBookService.create(eq(request.getFileModel())))
                .thenThrow(new UserException(new Message(errorCode, LOADED_CODE)));

        try {
            loader.load(List.of(request), LOADED_SUBJECT);

        } catch (UserException e) {
            fail();
        }
    }

    @Test
    public void testLoadWhenFileModelFailedOther() {

        RefBookDataRequest request = createRequest();

        final String errorCode = "refbook.with.code.other.error";
        when(refBookService.create(eq(request.getFileModel()))).thenThrow(new UserException(errorCode));

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

        RefBookDataRequest request = createRequest();

        final String errorCode = "some error is rethrowed";
        when(refBookService.create(eq(request.getFileModel()))).thenThrow(new NotFoundException(errorCode));

        try {
            loader.load(List.of(request), LOADED_SUBJECT);
            fail(getFailedMessage(NotFoundException.class));

        } catch (RuntimeException e) {
            assertEquals(NotFoundException.class, e.getClass());
            assertEquals(errorCode, getExceptionMessage(e));
        }
    }

    @Test
    public void testLoadWhenFileModelFailedUnknown() {

        RefBookDataRequest request = createRequest();

        final String errorCode = "unknown error is wrapped";
        when(refBookService.create(eq(request.getFileModel()))).thenThrow(new RuntimeException(errorCode));

        try {
            loader.load(List.of(request), LOADED_SUBJECT);
            fail(getFailedMessage(UserException.class));

        } catch (UserException e) {
            assertEquals(UserException.class, e.getClass());
            assertNotNull(e.getCause());
            assertEquals(RuntimeException.class, e.getCause().getClass());
            assertEquals(errorCode, getExceptionMessage((RuntimeException) e.getCause()));
        }
    }

    private RefBookDataRequest createRequest() {

        RefBookDataRequest request = new RefBookDataRequest();

        FileModel fileModel = new FileModel("filePath", "fileName");
        request.setFileModel(fileModel);

        return request;
    }
}