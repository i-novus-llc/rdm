package ru.i_novus.ms.rdm.impl.file.export;

import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;
import ru.i_novus.ms.rdm.impl.file.TempFileUtil;
import ru.i_novus.ms.rdm.impl.file.process.XlsxPerRowProcessor;
import ru.i_novus.ms.rdm.impl.util.mappers.StructureRowMapper;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.service.FieldFactoryImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toRow;

/**
 * Created by znurgaliev on 06.08.2018.
 */
public class XlsxFileGenerateProcessTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testFileGenerate() throws IOException {

        final FieldFactory fieldFactory = new FieldFactoryImpl();
        Field d_a_id = fieldFactory.createField("ID", FieldType.INTEGER);
        Field d_a_id_null = fieldFactory.createField("ID_NULL", FieldType.INTEGER);
        Field d_a_name = fieldFactory.createField("NAME", FieldType.STRING);
        Field d_a_dateCol = fieldFactory.createField("DATE_COL", FieldType.DATE);
        Field d_a_boolCol = fieldFactory.createField("BOOL_COL", FieldType.BOOLEAN);
        Field d_a_floatCol = fieldFactory.createField("FLOAT_COL", FieldType.FLOAT);

        final List<Row> d_a_rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            d_a_rows.add(toRow(new LongRowValue(
                    d_a_id.valueOf(BigInteger.valueOf(i)),
                    d_a_id_null.valueOf(null),
                    d_a_name.valueOf("test_" + i),
                    d_a_dateCol.valueOf(LocalDate.ofYearDay(2000, i + 1)),
                    d_a_boolCol.valueOf(i % 2 == 0),
                    d_a_floatCol.valueOf(BigDecimal.valueOf(i + 0.1)) // Дробная часть обязательна для valueOf
            )));
        }

        TempFileUtil.updateTempSubdirectory();

        final Structure structure = createTestStructure();
        final List<Row> actual = new ArrayList<>();
        try (Archiver archiver = new Archiver();
             PerRowFileGenerator fileGenerator = new XlsxFileGenerator(d_a_rows.iterator(), structure)) {

            archiver.addEntry(fileGenerator, "Z001.xlsx");
            try (ZipInputStream zis = new ZipInputStream(archiver.getArchive());
                 XlsxPerRowProcessor processor = new XlsxPerRowProcessor(
                         new StructureRowMapper(structure, null), getTestRowsProcessor(actual)
                 )) {
                zis.getNextEntry();
                processor.process(() -> zis);
            }

            assertEquals(d_a_rows, actual);
        }
    }


    private Structure createTestStructure() {

        final Structure structure = new Structure();
        structure.setAttributes(asList(
                Structure.Attribute.build("ID", "ID", FieldType.INTEGER, "ID"),
                Structure.Attribute.build("NAME", "NAME", FieldType.STRING, "NAME"),
                Structure.Attribute.build("DATE_COL", "DATE_COL", FieldType.DATE, "DATE_COL"),
                Structure.Attribute.build("BOOL_COL", "BOOL_COL", FieldType.BOOLEAN, "DATE_COL"),
                Structure.Attribute.build("FLOAT_COL", "FLOAT_COL", FieldType.FLOAT, "FLOAT_COL")
        ));
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