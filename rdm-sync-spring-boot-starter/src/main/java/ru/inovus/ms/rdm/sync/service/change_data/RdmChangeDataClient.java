package ru.inovus.ms.rdm.sync.service.change_data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Клиент для экспорта данных в RDM.
 */
public abstract class RdmChangeDataClient {

    @Autowired protected RdmChangeDataRequestCallback callback;
    @Autowired private RdmSyncDao dao;

    /**
     * Экспортировать данные в RDM (синхронно или через очередь сообщений, в зависимости от реализации).
     * В зависимости от результатов операции будет вызван соответствующий метод у {@link RdmChangeDataRequestCallback}.
     * @param refBookCode Код справочника
     * @param addUpdate Записи, которые нужно добавить/изменить в RDM
     * @param delete Записи, которые нужно удалить из RDM
     * @param <T> Этот параметр должен реализовывать интерфейс Serializable ({@link java.util.Map} отлично подойдет).
     */
    public abstract <T extends Serializable> void changeData(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete);

    /**
     * Вставить/Обновить записи в локальной таблице. Существующие записи и новые записи (проверяется по первичному ключу) из состояния {@link ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState#SYNCED} переходят в состояние
     * {@link ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState#DIRTY}. Со временем они перейдут в состояние {@link ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState#PENDING}.
     * Откуда они могут перейти либо обратно в состояние {@link ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState#SYNCED}, либо в состояние {@link ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState#ERROR}.
     * @param addUpdate Записи, которые нужно вставить/изменить в локальной таблице и, со временем, вставить/изменить в RDM.
     * @param localTable Локальная таблица с данными (с явно указанными схемой и названием таблицы)
     * @param <T> Этот параметр должен реализовывать интерфейс Serializable (для единообразия)
     */
    @Transactional
    public <T extends Serializable> void lazyInsertData(List<? extends T> addUpdate, String localTable) {
        VersionMapping versionMapping = dao.getVersionMappings().stream().filter(vm -> vm.getTable().equals(localTable)).findAny().orElseThrow(() -> new RdmException("No table " + localTable + " found."));
        String pk = versionMapping.getPrimaryField();
        String isDeletedField = versionMapping.getDeletedField();
        List<Pair<String, String>> schema = dao.getColumnNameAndDataTypeFromLocalDataTable(localTable);
        Map<String, Object>[] arr = Utils.mapForPgBatchInsert(addUpdate, schema);
        for (Map<String, Object> m : arr) {
            Object pv = m.get(pk);
            if (pv == null)
                throw new RdmException("No primary key found. Primary field: " + pk);
            if (!dao.isIdExists(localTable, pk, pv))
                dao.insertRow(localTable, m, false);
            else {
                dao.markDeleted(localTable, pk, isDeletedField, pv, false, false);
                dao.updateRow(localTable, pk, isDeletedField, m, false);
            }
        }
    }

}
