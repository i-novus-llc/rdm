package ru.i_novus.ms.rdm.impl.strategy.data;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.UnversionedBaseStrategyTest;
import ru.i_novus.ms.rdm.impl.strategy.data.api.AddRowValuesStrategy;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.i_novus.ms.rdm.impl.util.StructureTestConstants.ID_ATTRIBUTE_CODE;

public class UnversionedAddRowValuesStrategyTest extends UnversionedBaseStrategyTest {

    private static final String NAME_FIELD_DELETED_VALUE_SUFFIX = "_deleted";

    @InjectMocks
    private UnversionedAddRowValuesStrategy strategy;

    @Mock
    private AddRowValuesStrategy addRowValuesStrategy;

    @Test
    public void testAdd() {

        final RefBookVersionEntity entity = createUnversionedEntity();

        final List<RowValue> rowValues = List.of(
                // Без существующего конфликта:
                createRowValue(null, 1), // Добавление произвольной записи
                // С существующим конфликтом:
                createRowValue(null, 2), // Восстановление оригинальной записи
                createRowValue(null, 3)  // Добавление записи в изменённом виде
        );

        final List<RowValue> refRowValues = rowValues.stream()
                .map(rowValue -> {
                    BigInteger idValue = (BigInteger) rowValue.getFieldValue(ID_ATTRIBUTE_CODE).getValue();
                    return createReferrerRowValue(
                            idValue.longValue() * REFERRER_SYSTEM_ID_MULTIPLIER, idValue.intValue()
                    );
                })
                .collect(toList());
        refRowValues.stream()
                .filter(rowValue -> Objects.equals(rowValue.getSystemId(), 3 * REFERRER_SYSTEM_ID_MULTIPLIER))
                .forEach(rowValue -> {
                    Reference reference = (Reference) rowValue.getFieldValue(REFERRER_ATTRIBUTE_CODE).getValue();
                    reference.setDisplayValue(reference.getDisplayValue() + NAME_FIELD_DELETED_VALUE_SUFFIX);
                });

        strategy.add(entity, rowValues);

        verify(addRowValuesStrategy).add(eq(entity), eq(rowValues));
    }
}