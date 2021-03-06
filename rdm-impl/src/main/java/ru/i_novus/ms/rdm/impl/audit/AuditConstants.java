package ru.i_novus.ms.rdm.impl.audit;

import ru.i_novus.ms.rdm.impl.entity.PassportValueEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyList;

class AuditConstants {

    static final String OBJ_NAME_REFBOOK = "Справочник";
    static final String OBJ_TYPE_REFBOOK = "Reference book";

    static final String REFBOOK_CODE_KEY = "code";
    static final String REFBOOK_NAME_KEY = "name";
    static final String REFBOOK_SHORT_NAME_KEY = "shortName";
    static final String REFBOOK_VERSION_KEY = "version";
    static final String REFBOOK_STRUCTURE_KEY = "structure";

    static final String ABSENT_VALUE_SUBST = "-";

    static final Function<Object, String> GET_REFBOOK_ID_FROM_REFBOOK_VERSION_ENTITY = obj -> ((RefBookVersionEntity) obj).getRefBook().getCode();

    private AuditConstants() {}

    static Function<Object, Map<String, Object>> refBookCtxExtract(String...keys) {
        return obj -> {
            RefBookVersionEntity refBookVersion = (RefBookVersionEntity) obj;
            Map<String, Object> m = new HashMap<>();
            List<PassportValueEntity> passport = refBookVersion.getPassportValues() == null ? emptyList() : refBookVersion.getPassportValues();
            for (String s : keys) {
                switch (s) {
                    case REFBOOK_CODE_KEY:
                        m.put(REFBOOK_CODE_KEY, refBookVersion.getRefBook().getCode());
                        break;
                    case REFBOOK_NAME_KEY:
                        m.put(
                            REFBOOK_NAME_KEY,
                            passport.stream().filter(
                                p -> p.getAttribute().getCode().equals(REFBOOK_NAME_KEY)
                        ).findFirst().map(PassportValueEntity::getValue).orElse(ABSENT_VALUE_SUBST));
                        break;
                    case REFBOOK_SHORT_NAME_KEY:
                        m.put(
                            REFBOOK_SHORT_NAME_KEY,
                                passport.stream().filter(
                                p -> p.getAttribute().getCode().equals(REFBOOK_SHORT_NAME_KEY)
                        ).findFirst().map(PassportValueEntity::getValue).orElse(ABSENT_VALUE_SUBST));
                        break;
                    case REFBOOK_VERSION_KEY:
                        m.put(REFBOOK_VERSION_KEY, refBookVersion.getVersion());
                        break;
                    case REFBOOK_STRUCTURE_KEY:
                        m.put(REFBOOK_STRUCTURE_KEY, refBookVersion.getStructure());
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected key: " + s);
                }
            }
            return m;
        };
    }

}
