package ru.inovus.ms.rdm.file;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Reference;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NonStrictOnTypeRowMapperTest {


    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testMap() {
        int referenceVersion = -1;
        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setStructure(new Structure(Collections.singletonList(Structure.Attribute.build("count", "count",
                FieldType.INTEGER, false, "count")), null));
        when(versionRepository.findOne(eq(referenceVersion))).thenReturn(versionEntity);
        Map<String, Object> data = new LinkedHashMap() {{
            put("string", "abc");
            put("reference", "2");
            put("float", "2.1");
            put("date", "01.01.2011");
            put("boolean", "true");
        }};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate date = LocalDate.parse("01.01.2011", formatter);
        Map<String, Object> expectedData = new LinkedHashMap() {{
            put("string", "abc");
            put("reference", new Reference(versionEntity.getStorageCode(), ConverterUtil.date(versionEntity.getFromDate()), "count", "count", "2"));
            put("float", new BigDecimal("2.1"));
            put("date", date);
            put("boolean", Boolean.valueOf(true));
        }};
        Row expected = new Row(expectedData);

        NonStrictOnTypeRowMapper rowMapper = new NonStrictOnTypeRowMapper(createTestStructure(referenceVersion), versionRepository);
        Row actual = rowMapper.map(new Row(data));

        Assert.assertEquals(expected, actual);

    }

    @Test
    public void testMapWithWrongData(){
        int referenceVersion = -1;
        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setStructure(new Structure(Collections.singletonList(Structure.Attribute.build("count", "count",
                FieldType.INTEGER, false, "count")), null));
        when(versionRepository.findOne(eq(referenceVersion))).thenReturn(versionEntity);
        Map<String, Object> data = new LinkedHashMap() {{
            put("string", "abc");
            put("reference", "wrong value");
            put("float", "wrong value");
            put("date", "wrong value");
            put("boolean", "wrong value");
        }};

        NonStrictOnTypeRowMapper rowMapper = new NonStrictOnTypeRowMapper(createTestStructure(referenceVersion), versionRepository);
        Row actual = rowMapper.map(new Row(data));

        Assert.assertEquals(new Row(data), actual);

    }

    private Structure createTestStructure(int referenceVersion) {
        Structure structure = new Structure();
        structure.setAttributes(Arrays.asList(
                Structure.Attribute.build("string", "string", FieldType.STRING, false, "string"),
                Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, false, "count"),
                Structure.Attribute.build("float", "float", FieldType.FLOAT, false, "float"),
                Structure.Attribute.build("date", "date", FieldType.DATE, false, "date"),
                Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, false, "boolean")
        ));
        structure.setReferences(Collections.singletonList(new Structure.Reference("reference", referenceVersion, "count", Collections.singletonList("count"), null)));
        return structure;
    }
}
