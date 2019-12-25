package ru.inovus.ms.rdm.sync.service.change_data;

import org.springframework.data.util.Pair;
import org.springframework.util.ReflectionUtils;
import ru.inovus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.inovus.ms.rdm.api.model.refdata.Row;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static ru.inovus.ms.rdm.api.util.StringUtils.camelCaseToSnakeCase;
import static ru.inovus.ms.rdm.api.util.StringUtils.snakeCaseToCamelCase;

final class RdmSyncChangeDataUtils {

    private RdmSyncChangeDataUtils() {throw new UnsupportedOperationException();}

    static <T extends Serializable> RdmChangeDataRequest convertToRdmChangeDataRequest(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete) {
        List<Row> addUpdateRows = mapToRows(addUpdate);
        List<Row> deleteRows = mapToRows(delete);
        return new RdmChangeDataRequest(refBookCode, addUpdateRows, deleteRows);
    }

    static <T extends Serializable> List<Object> extractSnakeCaseKey(String snakeCaseKey, List<? extends T> ts) {
        if (ts.isEmpty())
            return emptyList();
        String key = snakeCaseToCamelCase(snakeCaseKey);
        List<Object> list = new ArrayList<>();
        for (T t : ts) {
            var v = new Object() {
                Object val = null;
            };
            if (t instanceof Map)
                v.val = ((Map) t).get(key);
            else {
                ReflectionUtils.doWithFields(t.getClass(), field -> {
                    if (field.getName().equals(key)) {
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

    private static <T extends Serializable> List<Row> mapToRows(List<? extends T> list) {
        if (list == null)
            return emptyList();
        return list.stream().map(t -> {
            if (t instanceof Map)
                return (Map) t;
            return tToMap(t, false, null, null);
        }).map(Row::new).collect(toList());
    }

    static <T extends Serializable> Map<String, Object>[] mapForPgBatchInsert(List<? extends T> list, List<Pair<String, String>> schema) {
        Set<String> columnsSnakeCase = schema.stream().map(Pair::getFirst).collect(toSet());
        Set<String> columnsCamelCase = schema.stream().map(pair -> snakeCaseToCamelCase(pair.getFirst())).collect(toSet());
        Map<String, Object>[] arr = new Map[list.size()];
        Iterator<? extends T> it = list.iterator();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = tToMap(it.next(), true, columnsCamelCase, columnsSnakeCase);
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
