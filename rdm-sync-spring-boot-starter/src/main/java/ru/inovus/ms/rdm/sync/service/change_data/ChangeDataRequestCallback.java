package ru.inovus.ms.rdm.sync.service.change_data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public interface ChangeDataRequestCallback {

    void onSuccess(String refBookCode, List<Object> addUpdate, List<Object> delete);
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
