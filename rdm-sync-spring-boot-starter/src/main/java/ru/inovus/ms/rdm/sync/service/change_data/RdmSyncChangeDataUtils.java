package ru.inovus.ms.rdm.sync.service.change_data;

import org.springframework.data.util.Pair;
import org.springframework.util.ReflectionUtils;
import ru.inovus.ms.rdm.sync.model.FieldMapping;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static ru.inovus.ms.rdm.api.util.StringUtils.camelCaseToSnakeCase;
import static ru.inovus.ms.rdm.api.util.StringUtils.snakeCaseToCamelCase;

final class RdmSyncChangeDataUtils {

    static final HashMap<String, Object> INTERNAL_TAG = new HashMap<>();

    private RdmSyncChangeDataUtils() {throw new UnsupportedOperationException();}

    static void reindex(List<FieldMapping> fieldMappings, Map<String, Object> map) {
        Set<String> sysProcessed = new HashSet<>();
        for (FieldMapping fieldMapping : fieldMappings) {
            String sys = fieldMapping.getSysField();
            String rdm = fieldMapping.getRdmField();
            if (sysProcessed.contains(rdm))
                continue;
            if (!sys.equals(rdm)) {
                Object v1 = map.get(sys);
                Object v2 = map.get(rdm);
                map.put(rdm, v1);
                if (v2 != null) {
                    map.put(sys, v2);
                }
            }
            sysProcessed.add(sys);
        }
    }

    @SuppressWarnings({"squid:S3776", "squid:S134"})
    static <T extends Serializable> List<Object> extractSnakeCaseKey(String snakeCaseKey, List<? extends T> ts) {
        if (ts.isEmpty())
            return emptyList();
        List<Object> list = new ArrayList<>();
        for (T t : ts) {
            var v = new Object() {
                Object val = null;
            };
            if (t instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) t;
                if (m.containsKey(snakeCaseKey))
                    v.val = m.get(snakeCaseKey);
                else {
                    for (Map.Entry<String, Object> e : m.entrySet()) {
                        if (camelCaseToSnakeCase(e.getKey()).equals(snakeCaseKey)) {
                            v.val = e.getValue();
                            break;
                        }
                    }
                }
            } else {
                ReflectionUtils.doWithFields(t.getClass(), field -> {
                    if (camelCaseToSnakeCase(field.getName()).equals(snakeCaseKey)) {
                        field.setAccessible(true);
                        v.val = field.get(t);
                    }
                });
            }
            if (v.val != null)
                list.add(v.val);
        }
        return list;
    }

    static <T extends Serializable> Map<String, Object>[] mapForPgBatchInsert(List<? extends T> list, List<Pair<String, String>> schema, IdentityHashMap<? super T, Map<String, Object>> identityHashMap) {
        Set<String> columnsSnakeCase = schema.stream().map(Pair::getFirst).collect(toSet());
        Set<String> columnsCamelCase = schema.stream().map(pair -> snakeCaseToCamelCase(pair.getFirst())).collect(toSet());
        Map<String, Object>[] arr = new Map[list.size()];
        Iterator<? extends T> it = list.iterator();
        for (int i = 0; i < arr.length; i++) {
            T t = it.next();
            arr[i] = tToMap(t, true, columnsCamelCase, columnsSnakeCase);
            identityHashMap.put(t, arr[i]);
        }
        return arr;
    }

    static <T extends Serializable> Map<String, Object> tToMap(T t, final boolean toSnakeCase, Set<String> schemaColumnsCamelCase, Set<String> schemaColumnsSnakeCase) {
        Map<String, Object> map = new HashMap<>();
        ReflectionUtils.doWithFields(t.getClass(), field -> {
            if (schemaColumnsCamelCase != null && !schemaColumnsCamelCase.contains(field.getName()))
                return;
            field.setAccessible(true);
            String key = toSnakeCase ? camelCaseToSnakeCase(field.getName()) : field.getName();
            map.put(key, field.get(t));
        });
        if (schemaColumnsSnakeCase != null) {
            for (String s : schemaColumnsSnakeCase) {
                map.putIfAbsent(s, null);
            }
        }
        return map;
    }

}
