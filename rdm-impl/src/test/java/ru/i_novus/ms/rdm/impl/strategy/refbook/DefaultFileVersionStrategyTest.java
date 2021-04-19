package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFileVersionStrategyTest {

    @InjectMocks
    private DefaultFileVersionStrategy defaultFileVersionStrategy;

    @Mock
    private VersionFileRepository versionFileRepository;

    private static final FileType TYPE_XML = FileType.XML;

    @Test
    public void testSaveDraftRefBook() {
        RefBookVersion refBookVersion = getRefBookVersion(RefBookVersionStatus.DRAFT);

        VersionFileEntity versionFileEntity = getVersionFileEntity();

        when(versionFileRepository.findByVersionIdAndType(refBookVersion.getId(), TYPE_XML))
                .thenReturn(versionFileEntity);

        defaultFileVersionStrategy.save(refBookVersion, TYPE_XML, "/");

        verify(versionFileRepository, times(0)).save(argThat(entity ->
                entity.getVersion().getId().equals(51) && entity.getType().equals(TYPE_XML)
        ));
    }

    @Test
    public void testSaveRefBookNotDraft() {
        RefBookVersion refBookVersion = getRefBookVersion(RefBookVersionStatus.PUBLISHED);

        when(versionFileRepository.findByVersionIdAndType(refBookVersion.getId(), TYPE_XML))
                .thenReturn(null);

        defaultFileVersionStrategy.save(refBookVersion, TYPE_XML, "/");

        verify(versionFileRepository, times(1)).save(argThat(entity ->
                entity.getVersion().getId().equals(51) && entity.getType().equals(TYPE_XML)
        ));
    }

    private RefBookVersion getRefBookVersion(RefBookVersionStatus refBookVersionStatus) {
        RefBookVersion refBookVersion = new RefBookVersion();
        refBookVersion.setId(51);
        refBookVersion.setStatus(refBookVersionStatus);

        return refBookVersion;
    }

    private VersionFileEntity getVersionFileEntity() {
        VersionFileEntity entity = new VersionFileEntity();
        entity.setId(12);
        entity.setType(FileType.XML);
        return entity;
    }
}