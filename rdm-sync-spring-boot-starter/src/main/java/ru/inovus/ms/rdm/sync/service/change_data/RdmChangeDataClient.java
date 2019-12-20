package ru.inovus.ms.rdm.sync.service.change_data;

import org.springframework.beans.factory.annotation.Autowired;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class RdmChangeDataClient {

    @Autowired protected RdmChangeDataRequestCallback callback;
    @Autowired private RdmSyncDao dao;

    public abstract <T extends Serializable> void changeData(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete);

    public <T extends Serializable> void lazyInsertData(List<? extends T> addUpdate, String localTable) {
        Map<String, Object>[] maps = Utils.mapForPgBatchInsert(addUpdate, dao.getColumnNameAndDataTypeFromLocalDataTable(localTable));
        dao.insertRows(localTable, maps);
    }

    public <T extends Serializable> void lazyInsertData(List<? extends T> addUpdate, Consumer<? super T> localSaver) {
        for (T t : addUpdate) {
            localSaver.accept(t);
        }
    }

}
