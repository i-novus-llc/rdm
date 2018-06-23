package ru.inovus.ms.rdm.enumeration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ru.inovus.ms.rdm.service.Identifiable;
import ru.inovus.ms.rdm.util.EnumDeserializer;

import java.util.Arrays;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonDeserialize(using = RefBookStatus.Deserializer.class)
public enum RefBookStatus implements Identifiable {

    PUBLISHED   (1, "Опубликован"),
    DRAFT       (2, "Черновик"),
    ARCHIVED    (3, "Архив");

    private Integer id;
    private String name;

    RefBookStatus(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static RefBookStatus valueOf(Integer id) {
        return Arrays.stream(values()).filter(v -> v.id.equals(id)).findAny()
                .orElse(null);
    }

    public static class Deserializer extends EnumDeserializer<RefBookStatus> {
        public Deserializer() {
            super(RefBookStatus.class);
        }
    }
}
