package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.UnversionedRefBookEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.RefBookLockService;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.time.LocalDateTime;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus.PUBLISHED;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedBasePublishStrategyTest {

    private static final int REFBOOK_ID = 2;
    private static final String REFBOOK_CODE = "refbook_code";

    private static final String STORAGE_CODE = "storage-code";

    @InjectMocks
    private UnversionedBasePublishStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private RefBookLockService refBookLockService;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private AfterPublishStrategy afterPublishStrategy;

    @Test
    public void testPublish() {

        RefBookVersionEntity draftEntity = createVersionEntity();

        RefBookVersionEntity expected = createVersionEntity();
        expected.setStatus(PUBLISHED);
        LocalDateTime fromDate = LocalDateTime.now();
        expected.setFromDate(fromDate);

        PublishResponse result = publish(draftEntity, fromDate, null);
        assertNotNull(result);
        assertEquals(REFBOOK_CODE, result.getRefBookCode());
        assertEquals(expected.getId(), result.getOldId());
        assertEquals(expected.getId(), result.getNewId());

        ArgumentCaptor<RefBookVersionEntity> savedCaptor = ArgumentCaptor.forClass(RefBookVersionEntity.class);
        verify(versionRepository).save(savedCaptor.capture());

        expected.setLastActionDate(savedCaptor.getValue().getLastActionDate());
        assertEquals(expected, savedCaptor.getValue());
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity refBookEntity = new UnversionedRefBookEntity();
        refBookEntity.setId(REFBOOK_ID);
        refBookEntity.setCode(REFBOOK_CODE);

        return refBookEntity;
    }

    private RefBookVersionEntity createVersionEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(1);
        entity.setStorageCode(STORAGE_CODE);
        entity.setRefBook(createRefBookEntity());
        entity.setStatus(PUBLISHED);
        entity.setStructure(createStructure());

        return entity;
    }

    private Structure createStructure() {

        return new Structure(
                asList(
                        Structure.Attribute.build("Code", "Код", FieldType.INTEGER, "Код записи"),
                        Structure.Attribute.build("Name", "Имя", FieldType.STRING, "Наименование"),
                        Structure.Attribute.build("Rec_Date", "Дата", FieldType.DATE, "Дата записи"),
                        Structure.Attribute.build("Count", "Счётчик", FieldType.FLOAT, "Количество")
                ),
                null);
    }

    private PublishResponse publish(RefBookVersionEntity entity, LocalDateTime fromDate, LocalDateTime toDate) {

        PublishRequest request = new PublishRequest(null);
        request.setFromDate(fromDate);
        request.setToDate(toDate);

        PublishResponse result = strategy.publish(entity, request);
        if (result != null) {
            assertFalse(request.getResolveConflicts());
        }

        return result;
    }
}