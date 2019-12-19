package ru.inovus.ms.rdm.sync.service.change_data;

import org.springframework.util.ReflectionUtils;
import ru.inovus.ms.rdm.api.model.refdata.ChangeDataRequest;
import ru.inovus.ms.rdm.api.model.refdata.Row;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public interface ChangeDataClient {

    void changeData(String refBookCode, List<Object> addUpdate, List<Object> delete);

    static ChangeDataRequest convertToChangeDataRequest(String refBookCode, List<Object> addUpdate, List<Object> delete) {
        List<Row> addUpdateRows = mapToRow(addUpdate);
        List<Row> deleteRows = mapToRow(delete);
        return new ChangeDataRequest(refBookCode, addUpdateRows, deleteRows);
    }

    private static List<Row> mapToRow(List<Object> list) {
        if (list == null)
            return emptyList();
        return list.stream().map(obj -> {
            if (obj instanceof Map)
                return (Map) obj;
            Map<String, Object> map = new HashMap<>();
            ReflectionUtils.doWithFields(obj.getClass(), field -> {
                field.setAccessible(true);
                map.put(field.getName(), field.get(obj));
            });
            return map;
        }).map(Row::new).collect(toList());
    }

}
