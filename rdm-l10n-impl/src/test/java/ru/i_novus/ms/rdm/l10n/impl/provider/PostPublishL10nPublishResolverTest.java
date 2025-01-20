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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        final PostPublishRequest request = new PostPublishRequest();
        resolver.resolve(request);

        verify(draftDataService).applyLocalizedDraft(
                anyString(), anyString(), anyString(),
                any(LocalDateTime.class), any(LocalDateTime.class)
        );

        verifyNoMoreInteractions(draftDataService);
    }

    @Test
    public void testResolveNull() {

        resolver.resolve(null);

        verifyNoMoreInteractions(draftDataService);
    }
}