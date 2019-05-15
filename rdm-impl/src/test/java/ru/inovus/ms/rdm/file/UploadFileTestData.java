package ru.inovus.ms.rdm.file;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.RefBookRowValue;
import ru.inovus.ms.rdm.model.Structure;

import java.math.BigInteger;
import java.util.*;

import static java.util.Arrays.asList;

public class UploadFileTestData {

    public static final String REFERENCE_ENTITY_CODE = "REF_CODE_TO_REFERENCE";
    public static final Integer REFERENCE_ENTITY_BOOK_ID = -10;
    public static final Integer REFERENCE_ENTITY_VERSION_ID = -11;
    public static final String REFERENCE_FIELD_CODE = "code";
    public static final Integer REFERENCE_FIELD_VALUE_1 = 2;
    public static final Integer REFERENCE_FIELD_VALUE_2 = 5;

    public static Structure createStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.buildPrimary("string", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build("integer", "integer", FieldType.INTEGER, "число"),
                        Structure.Attribute.build("date", "date", FieldType.DATE, "дата"),
                        Structure.Attribute.build("boolean", "boolean", FieldType.BOOLEAN, "булево"),
                        Structure.Attribute.build("float", "float", FieldType.FLOAT, "дробное"),
                        Structure.Attribute.build("reference", "reference", FieldType.REFERENCE, "ссылка")
                ),
                Collections.singletonList(
                        new Structure.Reference("reference", REFERENCE_ENTITY_CODE, null, null)
                )
        );
    }

    public static Structure createReferenceStructure() {
        Structure.Attribute referenceAttribute = Structure.Attribute.buildPrimary(REFERENCE_FIELD_CODE, "код", FieldType.INTEGER, null);

        return new Structure(
                Collections.singletonList(referenceAttribute),
                Collections.emptyList()
        );
    }

    public static RefBookEntity createReferenceBook() {
        RefBookEntity referenceBook = new RefBookEntity();
        referenceBook.setId(UploadFileTestData.REFERENCE_ENTITY_BOOK_ID);

        return referenceBook;
    }

    public static RefBookVersionEntity createReferenceVersion() {
        RefBookVersionEntity referenceVersion = new RefBookVersionEntity();
        referenceVersion.setId(UploadFileTestData.REFERENCE_ENTITY_VERSION_ID);
        referenceVersion.setRefBook(createReferenceBook());
        referenceVersion.setStructure(createReferenceStructure());

        return referenceVersion;
    }

    public static PageImpl<RefBookRowValue> createReferenceRows() {
        return new PageImpl<>( asList(
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(REFERENCE_FIELD_CODE, BigInteger.valueOf(REFERENCE_FIELD_VALUE_1))
                ), 1),
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(REFERENCE_FIELD_CODE, BigInteger.valueOf(REFERENCE_FIELD_VALUE_2))
                ), 1)
        ), new PageRequest(0, 10), 2);
    }

    public static Map<String, String> createPassport() {
        return new LinkedHashMap<>() {{
            put("name", "наименование справочника");
            put("shortName", "краткое наим-ие");
            put("description", "описание");
        }};
    }

    public static List<PassportValueEntity> createPassportValues(RefBookVersionEntity version) {
        return asList(
                new PassportValueEntity(new PassportAttributeEntity("name"), "наименование справочника", version),
                new PassportValueEntity(new PassportAttributeEntity("shortName"), "краткое наим-ие", version),
                new PassportValueEntity(new PassportAttributeEntity("description"), "описание", version)
        );
    }

}
