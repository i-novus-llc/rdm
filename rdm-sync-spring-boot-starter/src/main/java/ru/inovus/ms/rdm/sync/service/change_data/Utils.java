package ru.inovus.ms.rdm.sync.service.change_data;

import com.google.common.base.CaseFormat;
import org.springframework.data.util.Pair;
import org.springframework.util.ReflectionUtils;
import ru.inovus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.inovus.ms.rdm.api.model.refdata.Row;

import java.io.Serializable;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

final class Utils {

    private Utils() {throw new UnsupportedOperationException();}

    static <T extends Serializable> RdmChangeDataRequest convertToRdmChangeDataRequest(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete) {
        List<Row> addUpdateRows = mapToRows(addUpdate);
        List<Row> deleteRows = mapToRows(delete);
        return new RdmChangeDataRequest(refBookCode, addUpdateRows, deleteRows);
    }

    private static <T extends Serializable> List<Row> mapToRows(List<? extends T> list) {
        if (list == null)
            return emptyList();
        return list.stream().map(t -> {
            if (t instanceof Map)
                return (Map) t;
            return tToMap(t, false, null);
        }).map(Row::new).collect(toList());
    }

    static <T extends Serializable> Map<String, Object>[] mapForPgBatchInsert(List<? extends T> list, List<Pair<String, String>> schema) {
        Set<String> columns = schema.stream().map(pair -> snakeCaseToCamelCase(pair.getFirst())).collect(toSet());
        Map<String, Object>[] arr = new Map[list.size()];
        Iterator<? extends T> it = list.iterator();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = tToMap(it.next(), true, columns);
        }
        return arr;
    }

    private static String camelCaseToSnakeCase(String s) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, s);
    }

    private static String snakeCaseToCamelCase(String s) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, s);
    }

    private static <T extends Serializable> Map<String, Object> tToMap(T t, final boolean toSnakeCase, Set<String> takeOnly) {
        Map<String, Object> map = new HashMap<>();
        ReflectionUtils.doWithFields(t.getClass(), field -> {
            if (takeOnly != null && !takeOnly.contains(field.getName()))
                return;
            field.setAccessible(true);
            String key = toSnakeCase ? camelCaseToSnakeCase(field.getName()) : field.getName();
            map.put(key, field.get(t));
        });
        return map;
    }

}
