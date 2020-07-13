package ru.i_novus.ms.rdm.impl.util.mappers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class StructureRowMapperTest {

    @Mock
    private RefBookVersionRepository refBookVersionRepository;

    @Test
    public void testOurSuperUniversalRowMapper() {
        String refererCode = "REFERER_CODE";
        String refererStorageCode = "666";
        LocalDateTime refererPublishDateTime = LocalDate.of(1970, 01, 01).atStartOfDay();
        RefBookVersionEntity refererVersion = new RefBookVersionEntity();
        refererVersion.setRefBook(new RefBookEntity());
        refererVersion.getRefBook().setCode(refererCode);
        refererVersion.setStorageCode(refererStorageCode);
        refererVersion.setFromDate(refererPublishDateTime);
        refererVersion.setStructure(new Structure(List.of(Structure.Attribute.buildPrimary("REF_HERE", "-", FieldType.INTEGER, "-")), emptyList()));
        Mockito.when(refBookVersionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq(refererCode), any())).thenReturn(
            refererVersion
        );
        Structure structure = new Structure(List.of(
                Structure.Attribute.build("INTEGER", "-", FieldType.INTEGER, "-"),
                Structure.Attribute.build("STRING", "-", FieldType.STRING, "-"),
                Structure.Attribute.build("FLOAT", "-", FieldType.FLOAT, "-"),
                Structure.Attribute.build("BOOLEAN", "-", FieldType.BOOLEAN, "-"),
                Structure.Attribute.build("DATE", "-", FieldType.DATE, "-"),
                Structure.Attribute.build("REFERENCE", "-", FieldType.REFERENCE, "-")
        ), List.of(
                new Structure.Reference("REFERENCE", refererCode, "-")
        ));
        StructureRowMapper mapper = new StructureRowMapper(structure, refBookVersionRepository);
        testField(mapper, "INTEGER", BigInteger.valueOf(123), "123", BigInteger.valueOf(123L), (short) 123, (byte) 123, 123L, 123);
        testField(mapper, "STRING", "str", "str");
        testField(mapper, "FLOAT", BigDecimal.valueOf(123.6), 123.6f, 123.6d, "123.6", "123,6", BigDecimal.valueOf(123.6));
        testField(mapper, "BOOLEAN", true, "true", true);
        testField(mapper, "DATE", LocalDate.of(2019, 12, 12), "2019-12-12", "12.12.2019", LocalDate.of(2019, 12, 12), LocalDate.of(2019, 12, 12).atStartOfDay(), new Date(2019 - 1900, Calendar.DECEMBER, 12));
        testField(mapper, "REFERENCE", new Reference(refererStorageCode, refererPublishDateTime, "REF_HERE", new DisplayExpression("-"), "2"), "2", 2, (short) 2, (byte) 2, BigInteger.valueOf(2), 2L);
    }

    private void testField(StructureRowMapper mapper, String fieldCode, Object expectedVal, Object...values) {
        for (Object value : values) {
            Row row = new Row(new HashMap<>(Map.of(fieldCode, value)));
            mapper.map(row);
            assertEquals(row.getData().get(fieldCode), expectedVal);
        }
    }

}
