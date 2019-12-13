package ru.inovus.ms.rdm.impl.file;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.IntegerFieldValue;
import ru.inovus.ms.rdm.impl.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.impl.entity.PassportValueEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookEntity;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.Structure;

import java.math.BigInteger;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

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
                singletonList(
                        new Structure.Reference("reference", REFERENCE_ENTITY_CODE, null)
                )
        );
    }

    public static Structure createReferenceStructure() {
        Structure.Attribute referenceAttribute = Structure.Attribute.buildPrimary(REFERENCE_FIELD_CODE, "код", FieldType.INTEGER, null);

        return new Structure(
                singletonList(referenceAttribute),
                emptyList()
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
        return new PageImpl<>(asList(
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(REFERENCE_FIELD_CODE, BigInteger.valueOf(REFERENCE_FIELD_VALUE_1))
                ), 1),
                new RefBookRowValue(new LongRowValue(
                        new IntegerFieldValue(REFERENCE_FIELD_CODE, BigInteger.valueOf(REFERENCE_FIELD_VALUE_2))
                ), 1)
        ), PageRequest.of(0, 10), 2);
    }

    public static Map<String, Object> createPassport() {
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
