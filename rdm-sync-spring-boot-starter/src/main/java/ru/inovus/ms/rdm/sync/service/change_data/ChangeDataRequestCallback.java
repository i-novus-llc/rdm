package ru.inovus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * В общем случае, вам нужно думать о методах этого интерфейса, как об UNDO/REDO (то бишь Ctrl+Z/Ctrl+Y).
 * onSuccess -- это REDO, onError -- это UNDO. Желательно сделать методы идемпотентными, то есть если кто - то
 * вызовет onSuccess несколько раз -- результат этих вызовов будет такой же, как если бы кто - то вызвал его ровно один раз.
 */
public interface ChangeDataRequestCallback {

    /**
     * Этот метод будет вызван, если изменения применились в RDM.
     */
    void onSuccess(String refBookCode, List<Object> addUpdate, List<Object> delete);

    /**
     * Этот метод будет вызван, если RDM вернул ошибку, не связанную с блокировками справочников или произошел таймаут соединения.
     * Таким образом, даже если ваши изменения могли пройти в RDM (то бишь по валидациям и т.п), но в RDM что - то пошло не так
     * (скажем произошел OutOfMemoryError) -- запись там не появится. Если ошибка таймаутовая -- будет вызван этот метод
     * (однако изменения могут как появится, так и нет).
     */
    void onError(String refBookCode, List<Object> addUpdate, List<Object> delete, Exception ex);

    class DefaultChangeDataRequestCallback implements ChangeDataRequestCallback {

        private static final Logger logger = LoggerFactory.getLogger(DefaultChangeDataRequestCallback.class);

        @Override
        public void onSuccess(String refBookCode, List<Object> addUpdate, List<Object> delete) {
            logger.info("Successfully pulled into RDM into refBook with code {}. Payload:\nAdded/Update objects: {},\nDeleted objects: {}", refBookCode, addUpdate, delete);
        }

        @Override
        public void onError(String refBookCode, List<Object> addUpdate, List<Object> delete, Exception ex) {
            logger.error("Error occurred while pulling data into RDM into refBook with code {}. Payload:\nAttempt to add/update objects: {},\nAttempt to delete objects: {}", refBookCode, addUpdate, delete, ex);
        }

    }

}
