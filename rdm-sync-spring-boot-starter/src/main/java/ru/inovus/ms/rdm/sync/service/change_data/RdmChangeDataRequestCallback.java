package ru.inovus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.io.Serializable;
import java.util.List;

/**
 * В общем случае, вам нужно думать о методах этого интерфейса, как об UNDO/REDO (то бишь Ctrl+Z/Ctrl+Y).
 * onSuccess -- это REDO, onError -- это UNDO. Желательно сделать методы идемпотентными, то есть если кто - то
 * вызовет onSuccess несколько раз -- результат этих вызовов будет такой же, как если бы кто - то вызвал его ровно один раз.
 */
public abstract class RdmChangeDataRequestCallback {

    private static final Logger logger = LoggerFactory.getLogger(RdmChangeDataRequestCallback.class);

    @Autowired
    private RdmSyncDao dao;

    /**
     * Этот метод будет вызван, если изменения применились в RDM.
     */
    @Transactional
    public final <T extends Serializable> void onSuccess(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete) {
        if (casState(refBookCode, addUpdate, RdmSyncLocalRowState.SYNCED))
            onSuccess0(refBookCode, addUpdate, delete);
    }

    protected abstract <T extends Serializable> void onSuccess0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete);

    /**
     * Этот метод будет вызван, если RDM вернул ошибку, не связанную с блокировками справочников или произошел таймаут соединения.
     * Таким образом, даже если ваши изменения могли пройти в RDM (то бишь по валидациям и т.п), но в RDM что - то пошло не так
     * (скажем произошел OutOfMemoryError) -- запись там не появится. Если ошибка таймаутовая -- будет вызван этот метод
     * (однако изменения могут как появится, так и нет).
     */
    @Transactional
    public final <T extends Serializable> void onError(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete, Exception ex) {
        if (casState(refBookCode, addUpdate, RdmSyncLocalRowState.ERROR))
            onError0(refBookCode, addUpdate, delete, ex);
    }

    protected abstract <T extends Serializable> void onError0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete, Exception ex);

    private <T extends Serializable> boolean casState(String refBookCode, List<? extends T> addUpdate, RdmSyncLocalRowState state) {
        boolean stateChanged = true;
        VersionMapping vm = dao.getVersionMapping(refBookCode);
        if (vm != null) {
            String pk = vm.getPrimaryField();
            String table = vm.getTable();
            List<Object> pks = RdmSyncChangeDataUtils.extractSnakeCaseKey(pk, addUpdate);
            dao.disableInternalLocalRowStateUpdateTrigger(vm.getTable());
            try {
                stateChanged = dao.setLocalRecordsState(table, pk, pks, RdmSyncLocalRowState.PENDING, state);
            } catch (Exception ex) {
                stateChanged = false;
                logger.info("State change did not pass. Skipping request on {}.", refBookCode, ex);
            }
            dao.enableInternalLocalRowStateUpdateTrigger(vm.getTable());
        }
        return stateChanged;
    }

    public static class DefaultRdmChangeDataRequestCallback extends RdmChangeDataRequestCallback {

        private static final Logger logger = LoggerFactory.getLogger(DefaultRdmChangeDataRequestCallback.class);

        @Override
        public <T extends Serializable> void onSuccess0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete) {
            logger.info("Successfully pulled into RDM into refBook with code {}. Payload:\nAdded/Update objects: {},\nDeleted objects: {}", refBookCode, addUpdate, delete);
        }

        @Override
        public <T extends Serializable> void onError0(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete, Exception ex) {
            logger.error("Error occurred while pulling data into RDM into refBook with code {}. Payload:\nAttempt to add/update objects: {},\nAttempt to delete objects: {}", refBookCode, addUpdate, delete, ex);
        }

    }

}
