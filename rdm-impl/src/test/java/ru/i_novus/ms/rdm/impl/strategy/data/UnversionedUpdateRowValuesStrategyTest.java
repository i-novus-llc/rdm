package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.ms.rdm.impl.strategy.data.api.UpdateRowValuesStrategy;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.math.BigInteger;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.ID_ATTRIBUTE_CODE;

public class UnversionedUpdateRowValuesStrategyTest extends UnversionedBaseStrategyTest {

    protected static final String NAME_FIELD_RESTORED_VALUE_SUFFIX = "_restored";
    protected static final String NAME_FIELD_CHANGED_VALUE_SUFFIX = "_changed";

    @InjectMocks
    private UnversionedUpdateRowValuesStrategy strategy;

    @Mock
    private UpdateRowValuesStrategy updateRowValuesStrategy;

    @Test
    public void testUpdate() {

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
                createRowValue(6L, 6)  // Изменение записи в виде: удаление + добавление
        );

        final List<RowValue> newRowValues = oldRowValues.stream()
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return createUpdatedRowValue(idValue.intValue());
                })
                .collect(toList());

        // .update
        strategy.update(entity, oldRowValues, newRowValues);

        verify(updateRowValuesStrategy).update(eq(entity),
                eq(oldRowValues.subList(0, oldRowValues.size())),
                eq(newRowValues.subList(0, newRowValues.size()))
        );

        verifyNoMoreInteractions(updateRowValuesStrategy);
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