package ru.inovus.ms.rdm.impl.file.export;

import org.junit.Test;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.service.FieldFactoryImpl;
import ru.inovus.ms.rdm.impl.file.export.Archiver;
import ru.inovus.ms.rdm.impl.file.export.PerRowFileGenerator;
import ru.inovus.ms.rdm.impl.file.export.XlsFileGenerator;
import ru.inovus.ms.rdm.impl.file.process.RowsProcessor;
import ru.inovus.ms.rdm.impl.file.StructureRowMapper;
import ru.inovus.ms.rdm.impl.file.process.XlsPerRowProcessor;
import ru.inovus.ms.rdm.api.model.Result;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.Structure;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ru.inovus.ms.rdm.impl.util.ConverterUtil.toRow;

/**
 * Created by znurgaliev on 06.08.2018.
 */
public class XlsFileGenerateProcessTest {


    @Test
    public void testFileGenerate() throws IOException {
        FieldFactory fieldFactory = new FieldFactoryImpl();
        List<Field> d_a_fields = new ArrayList<>();
        Field d_a_id = fieldFactory.createField("ID", FieldType.INTEGER);
        Field d_a_id_null = fieldFactory.createField("ID_NULL", FieldType.INTEGER);
        Field d_a_name = fieldFactory.createField("NAME", FieldType.STRING);
        Field d_a_dateCol = fieldFactory.createField("DATE_COL", FieldType.DATE);
        Field d_a_boolCol = fieldFactory.createField("BOOL_COL", FieldType.BOOLEAN);
        Field d_a_floatCol = fieldFactory.createField("FLOAT_COL", FieldType.FLOAT);
        d_a_fields.add(d_a_id);
        d_a_fields.add(d_a_name);
        d_a_fields.add(d_a_dateCol);
        d_a_fields.add(d_a_boolCol);
        d_a_fields.add(d_a_floatCol);

        List<Row> d_a_rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            d_a_rows.add(toRow(new LongRowValue(
                    d_a_id.valueOf(BigInteger.valueOf(i)),
                    d_a_id_null.valueOf(null),
                    d_a_name.valueOf("test" + i),
                    d_a_dateCol.valueOf(LocalDate.ofYearDay(2000, i + 1)),
                    d_a_boolCol.valueOf(i % 2 == 0),
                    d_a_floatCol.valueOf(new BigDecimal((float) i)))));
        }
        Structure structure = createTestStructure();
        List<Row> actual = new ArrayList<>();

        try (Archiver archiver = new Archiver();
             PerRowFileGenerator fileGenerator = new XlsFileGenerator(d_a_rows.iterator(), structure)) {
            archiver.addEntry(fileGenerator, "Z001.xlsx");
            try (ZipInputStream zis = new ZipInputStream(archiver.getArchive());) {
                zis.getNextEntry();
                new XlsPerRowProcessor(new StructureRowMapper(structure, null), getTestRowsProcessor(actual)).process(() -> zis);
            }

            assertEquals(d_a_rows, actual);
        }
    }


    private Structure createTestStructure() {
        Structure structure = new Structure();
        structure.setAttributes(asList(
                Structure.Attribute.build("ID", "ID", FieldType.INTEGER, "ID"),
                Structure.Attribute.build("NAME", "NAME", FieldType.STRING, "NAME"),
                Structure.Attribute.build("DATE_COL", "DATE_COL", FieldType.DATE, "DATE_COL"),
                Structure.Attribute.build("BOOL_COL", "BOOL_COL", FieldType.BOOLEAN, "DATE_COL"),
                Structure.Attribute.build("FLOAT_COL", "FLOAT_COL", FieldType.FLOAT, "FLOAT_COL")));
        return structure;
    }

    private RowsProcessor getTestRowsProcessor(List<Row> actual) {

        return new RowsProcessor() {

            @Override
            public Result append(Row row) {
                actual.add(row);
                return null;
            }

            @Override
            public Result process() {
                return null;
            }
        };
    }
}