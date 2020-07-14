package ru.i_novus.ms.rdm.esnsi.file_gen;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.util.Collection;

public interface RefBookStructure {

    Collection<Attribute> attributes();
    Collection<Reference> references();

    interface Attribute {
        String code();
        String name();
        String description();
        FieldType type();
        boolean isPrimary();
    }

    interface Reference {
        String attribute();
        String referenceCode();
        String displayExpression();
    }

}
