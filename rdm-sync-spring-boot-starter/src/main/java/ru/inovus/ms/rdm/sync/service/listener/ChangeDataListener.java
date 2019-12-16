package ru.inovus.ms.rdm.sync.service.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import ru.inovus.ms.rdm.api.model.refdata.ChangeDataRequest;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.sync.service.change_data.ChangeDataRequestCallback;

import java.util.List;
import java.util.Map;

public class ChangeDataListener {

    private static final Logger logger = LoggerFactory.getLogger(ChangeDataListener.class);

    private final RefBookService refBookService;
    private final ChangeDataRequestCallback callback;

    public ChangeDataListener(RefBookService refBookService, ChangeDataRequestCallback callback) {
        this.refBookService = refBookService;
        this.callback = callback;
    }

    @JmsListener(destination = "${rdm_sync.change_data.queue:changeData}", containerFactory = "changeDataQueueMessageListenerContainerFactory")
    public void onChangeDataRequest(Map<String, Object> msg) {
        List<List<Object>> src = (List<List<Object>>) msg.get("converted");
        List<Object> addUpdate = src.get(0);
        List<Object> delete = src.get(1);
        ChangeDataRequest converted = (ChangeDataRequest) msg.get("converted");
        logger.info("Change data request on refBook with code {} arrived.", converted.getRefBookCode());
        try {
            refBookService.changeData(converted);
            callback.onSuccess(converted.getRefBookCode(), addUpdate, delete);
        } catch (Exception e) {
            callback.onError(converted.getRefBookCode(), addUpdate, delete, e);
        }
    }

}
