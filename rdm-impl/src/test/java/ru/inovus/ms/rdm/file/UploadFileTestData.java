package ru.inovus.ms.rdm.file;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.Structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class UploadFileTestData {

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
                new ArrayList<>() // NB: Убрать, когда переделаем ссылочность
        );
    }

    public static Structure createStringStructure() {
        return new Structure(
                asList(
                        Structure.Attribute.build("string", "string", FieldType.STRING, "строка"),
                        Structure.Attribute.build("integer", "integer", FieldType.STRING, "число"),
                        Structure.Attribute.build("date", "date", FieldType.STRING, "дата"),
                        Structure.Attribute.build("boolean", "boolean", FieldType.STRING, "булево"),
                        Structure.Attribute.build("float", "float", FieldType.STRING, "дробное"),
                        Structure.Attribute.build("reference", "reference", FieldType.STRING, "ссылка")
                ),
                new ArrayList<>() // NB: Убрать, когда переделаем ссылочность
        );
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
