package ru.i_novus.ms.rdm.l10n.impl.provider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.ms.rdm.l10n.impl.BaseTest;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PostPublishL10nPublishResolverTest extends BaseTest {

    @InjectMocks
    private PostPublishL10nPublishResolver resolver;

    @Mock
    private L10nDraftDataService draftDataService;

    @Test
    public void testResolve() {

        final PostPublishRequest request = createRequest();
        resolver.resolve(request);

        verify(draftDataService).applyLocalizedDraft(
                request.getLastStorageCode(),
                request.getOldStorageCode(), request.getNewStorageCode(),
                request.getFromDate(), request.getToDate()
        );

        verifyNoMoreInteractions(draftDataService);
    }

    private PostPublishRequest createRequest() {

        final PostPublishRequest request = new PostPublishRequest();
        request.setRefBookCode("ref_book_code");

        request.setLastStorageCode("last-storage-code");
        request.setOldStorageCode("old-storage-code");
        request.setNewStorageCode("new-storage-code");

        request.setFromDate(LocalDateTime.now());
        request.setToDate(LocalDateTime.now());

        return request;
    }

    @Test
    public void testResolveNull() {

        resolver.resolve(null);

        verifyNoMoreInteractions(draftDataService);
    }
}