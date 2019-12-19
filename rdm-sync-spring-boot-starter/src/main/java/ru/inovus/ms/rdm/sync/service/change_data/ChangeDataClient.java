package ru.inovus.ms.rdm.sync.service.change_data;

import java.util.List;

public interface ChangeDataClient {

    void changeData(String refBookCode, List<Object> addUpdate, List<Object> delete);

}
