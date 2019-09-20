package ru.inovus.ms.rdm.impl.file;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.impl.file.NonStrictOnTypeRowMapper;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.platform.datastorage.temporal.model.DisplayExpression.toPlaceholder;

@RunWith(MockitoJUnitRunner.class)
public class NonStrictOnTypeRowMapperTest {

    private final String referenceCode = "test_storage";
    private final int referenceVersionId = -1;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testMap() {
        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setStructure(new Structure(singletonList(Structure.Attribute.buildPrimary("count", "count",
                FieldType.INTEGER, "count")), null));
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq(referenceCode), eq(RefBookVersionStatus.PUBLISHED)))
                .thenReturn(versionEntity);

        Map<String, Object> data = new LinkedHashMap<>() {{
            put("string", "abc");
            put("reference", "2");
            put("float", "2.1");
            put("date", "01.01.2011");
            put("boolean", "true");
        }};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate date = LocalDate.parse("01.01.2011", formatter);

        Map<String, Object> expectedData = new LinkedHashMap<>() {{
            put("string", "abc");
            put("reference", new Reference(versionEntity.getStorageCode(), versionEntity.getFromDate(), "count", DisplayExpression.ofField("count"), "2"));
            put("float", new BigDecimal("2.1"));
            put("date", date);
            put("boolean", true);
        }};
        Row expected = new Row(expectedData);

        NonStrictOnTypeRowMapper rowMapper = new NonStrictOnTypeRowMapper(createTestStructure(referenceCode), versionRepository);
        Row actual = rowMapper.map(new Row(data));

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMapWithWrongData(){
        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setStructure(new Structure(singletonList(Structure.Attribute.build("count", "count",
                FieldType.INTEGER, "count")), null));

        RefBookVersionEntity m1VersionEntity = new RefBookVersionEntity();
        m1VersionEntity.setId(referenceVersionId);
        when(versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(eq(referenceCode), eq(RefBookVersionStatus.PUBLISHED))).thenReturn(m1VersionEntity);

        Map<String, Object> data = new LinkedHashMap<>() {{
            put("string", "abc");
            put("reference", "wrong value");
            put("float", "wrong value");
            put("date", "wrong value");
            put("boolean", "wrong value");
        }};

        NonStrictOnTypeRowMapper rowMapper = new NonStrictOnTypeRowMapper(createTestStructure(referenceCode), versionRepository);
        Row actual = rowMapper.map(new Row(data));

        Assert.assertEquals(new Row(data), actual);
    }

    private Structure createTestStructure(String referenceCode) {
        Structure structure = new Structure();
        structure.setAttributes(Arrays.asList(
                Structure.Attribute.build("string", "string", FieldType.STRING, "string"),
                Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, "count"),
                Structure.Attribute.build("float", "float", FieldType.FLOAT, "float"),
                Structure.Attribute.build("date", "date", FieldType.DATE, "date"),
                Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, "boolean")
        ));
        structure.setReferences(singletonList(new Structure.Reference("reference", referenceCode, toPlaceholder("count"))));

        return structure;
    }
}
