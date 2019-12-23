package ru.inovus.ms.rdm.sync.service.change_data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class RdmChangeDataClient {

    @Autowired protected RdmChangeDataRequestCallback callback;
    @Autowired private RdmSyncDao dao;

    public abstract <T extends Serializable> void changeData(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete);

    @Transactional
    public <T extends Serializable> void lazyInsertData(List<? extends T> addUpdate, String localTable) {
        String pk = dao.getVersionMappings().stream().filter(versionMapping -> versionMapping.getTable().equals(localTable)).map(VersionMapping::getPrimaryField).findAny().orElseThrow(() -> new RdmException("No table " + localTable + " found."));
        Map<String, Object>[] maps = Utils.mapForPgBatchInsert(addUpdate, dao.getColumnNameAndDataTypeFromLocalDataTable(localTable));
        dao.insertUpdateRows(localTable, pk, maps, true);
    }

    @Transactional
    public <T extends Serializable> void lazyInsertData(List<? extends T> addUpdate, Consumer<? super T> localSaver) {
        for (T t : addUpdate) {
            localSaver.accept(t);
        }
    }

}
