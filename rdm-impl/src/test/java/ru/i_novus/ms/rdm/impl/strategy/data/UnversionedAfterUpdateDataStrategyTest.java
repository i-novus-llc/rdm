package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.ms.rdm.impl.strategy.publish.EditPublishStrategy;
import ru.i_novus.ms.rdm.impl.strategy.referrer.UnversionedAfterUpdateProcessReferrersStrategy;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.ID_ATTRIBUTE_CODE;

public class UnversionedAfterUpdateDataStrategyTest extends UnversionedBaseStrategyTest {

    protected static final String NAME_FIELD_RESTORED_VALUE_SUFFIX = "_restored";
    protected static final String NAME_FIELD_CHANGED_VALUE_SUFFIX = "_changed";

    @InjectMocks
    private UnversionedAfterUpdateDataStrategy strategy;

    @Mock
    private EditPublishStrategy editPublishStrategy;

    @Mock
    private UnversionedAfterUpdateProcessReferrersStrategy processReferrersStrategy;

    @Test
    public void testApply() {

        final RefBookVersionEntity entity = createUnversionedEntity();

        final List<RowValue> oldRowValues = List.of(
                // Без изменения:
                createRowValue(1L, 1), // Запись без изменения
                // С изменением значений непервичных атрибутов:
                createRowValue(2L, 2), // Восстановление записи без конфликта обновления
                createRowValue(3L, 3), // Восстановление записи с конфликтом обновления
                createRowValue(4L, 4), // Изменение записи без конфликта обновления
                createRowValue(5L, 5), // Изменение записи с конфликтом обновления
                // С изменением значения первичного ключа:
                createRowValue(6L, 6)  // Изменение записи в виде удаление + добавление
        );

        final List<RowValue> newRowValues = oldRowValues.stream()
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return createUpdatedRowValue(idValue.intValue());
                })
                .collect(toList());

        final List<RowValue> refRowValues = oldRowValues.stream()
                .filter(rowValue -> !rowValue.getSystemId().equals(6L))
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return createReferrerRowValue(
                            idValue.longValue() * REFERRER_SYSTEM_ID_MULTIPLIER, idValue.intValue()
                    );
                })
                .filter(Objects::nonNull)
                .collect(toList());
        refRowValues.stream()
                .filter(rowValue ->
                        List.of(2 * REFERRER_SYSTEM_ID_MULTIPLIER, 3 * REFERRER_SYSTEM_ID_MULTIPLIER)
                                .contains((Long) rowValue.getSystemId())
                )
                .forEach(rowValue -> {
                    Reference reference = (Reference) rowValue.getFieldValue(REFERRER_ATTRIBUTE_CODE).getValue();
                    reference.setDisplayValue(reference.getDisplayValue() + NAME_FIELD_RESTORED_VALUE_SUFFIX);
                });

        strategy.apply(entity, emptyList(), oldRowValues, newRowValues);

        verify(editPublishStrategy).publish(entity);
        verify(processReferrersStrategy).apply(eq(entity), any(), eq(oldRowValues), eq(newRowValues));
    }

    private RowValue createUpdatedRowValue(Integer id) {

        switch (id) {
            case 1: return createRowValue(id.longValue(), id);
            case 2:
            case 3: return createRowValue(id.longValue(), BigInteger.valueOf(id),
                        NAME_FIELD_VALUE_PREFIX + id + NAME_FIELD_RESTORED_VALUE_SUFFIX, TEXT_FIELD_VALUE_PREFIX + id);
            case 4:
            case 5: return createRowValue(id.longValue(), BigInteger.valueOf(id),
                        NAME_FIELD_VALUE_PREFIX + id + NAME_FIELD_CHANGED_VALUE_SUFFIX, TEXT_FIELD_VALUE_PREFIX + id);
            case 6: return createRowValue(id.longValue() + 1, BigInteger.valueOf(id),
                        NAME_FIELD_CHANGED_VALUE_SUFFIX + id, TEXT_FIELD_VALUE_PREFIX + id);
            default: return null;
        }
    }
}