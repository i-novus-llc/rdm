package ru.i_novus.ms.rdm.loader.client.service;

import net.n2oapp.platform.loader.client.LoadingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import ru.i_novus.ms.rdm.loader.client.BaseTest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static ru.i_novus.ms.rdm.api.util.loader.RefBookDataConstants.FIELD_REF_BOOK_FILE;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("rawtypes")
public class RefBookDataClientLoaderTest extends BaseTest {

    @InjectMocks
    private RefBookDataClientLoader loader;

    @Mock
    private RestOperations restTemplate;

    @Before
    public void setUp() {

        setField(loader, "endpointPattern", "/loaders/{subject}/{target}");
    }

    @Test
    public void testLoad() {

        final Resource jsonFile = new ClassPathResource("rdm.json");

        final ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.ACCEPTED);

        final ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        when(restTemplate.postForEntity(any(String.class), any(Object.class), eq(String.class))).thenReturn(response);

        loader.load(newUri(), "test", "refBookData", jsonFile);

        verify(restTemplate, times(2))
                .postForEntity(any(String.class), captor.capture(), eq(String.class));

        assertNotNull(captor);
        final List<Object> objValues = captor.getAllValues();
        assertEquals(2, objValues.size());

        objValues.forEach(this::testCaptorValue);
    }

    private void testCaptorValue(Object value) {

        assertTrue(value instanceof HttpEntity);

        final HttpEntity request = (HttpEntity) value;

        final Object body = request.getBody();
        assertNotNull(body);
        assertTrue(body instanceof MultiValueMap);

        @SuppressWarnings("unchecked")
        final MultiValueMap<String, Object> data = (MultiValueMap) body;
        // Тело должно содержать либо поле file, либо поля structure и data.
        assertEquals(data.containsKey(FIELD_REF_BOOK_FILE) ? 1 : 2, data.size() - 4);
    }

    @Test
    public void testLoadFailed() {

        Resource jsonFile = new ClassPathResource("rdm.json");

        ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        when(restTemplate.postForEntity(any(String.class), captor.capture(), eq(String.class))).thenReturn(response);

        try {
            loader.load(newUri(), "test", "refBookData", jsonFile);
            fail(getFailedMessage(LoadingException.class));

        } catch (RuntimeException e) {
            assertEquals(LoadingException.class, e.getClass());
            assertNotNull(getExceptionMessage(e));
        }

        assertNotNull(captor);
        List<Object> objValues = captor.getAllValues();
        assertEquals(1, objValues.size());
    }

    private URI newUri() {
        try {
            return new URI("localhost");

        } catch (URISyntaxException e) {
            return null;
        }
    }
}