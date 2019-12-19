package ru.inovus.ms.rdm.sync.service.listener;

import net.n2oapp.platform.jaxrs.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.refdata.ChangeDataRequest;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.sync.service.change_data.ChangeDataRequestCallback;

import java.util.List;
import java.util.Set;

public class ChangeDataListener {

    private static final Logger logger = LoggerFactory.getLogger(ChangeDataListener.class);

    private static final Set<String> CONCURRENCY_ISSUES = Set.of("refbook.lock.draft.is.publishing", "refbook.lock.draft.is.updating");

    private final RefBookService refBookService;
    private final ChangeDataRequestCallback callback;

    public ChangeDataListener(RefBookService refBookService, ChangeDataRequestCallback callback) {
        this.refBookService = refBookService;
        this.callback = callback;
    }

    @JmsListener(destination = "${rdm_sync.change_data.queue:changeData}", containerFactory = "changeDataQueueMessageListenerContainerFactory")
    public void onChangeDataRequest(List<Object> msg) {
        List<List<Object>> src = (List<List<Object>>) msg.get(0);
        List<Object> addUpdate = src.get(0);
        List<Object> delete = src.get(1);
        ChangeDataRequest converted = (ChangeDataRequest) msg.get(1);
        logger.info("Change data request on refBook with code {} arrived.", converted.getRefBookCode());
        try {
            refBookService.changeData(converted);
            callback.onSuccess(converted.getRefBookCode(), addUpdate, delete);
        } catch (RestException re) {
            boolean concurrencyIssue = false;
            if (re.getErrors() != null)
                concurrencyIssue = re.getErrors().stream().anyMatch(error -> CONCURRENCY_ISSUES.contains(error.getMessage()));
            concurrencyIssue |= CONCURRENCY_ISSUES.contains(re.getMessage());
            if (!concurrencyIssue)
                callback.onError(converted.getRefBookCode(), addUpdate, delete, re);
            else
                throw new RdmException();
        } catch (Exception e) {
            logger.error("Error occurred while pulling changes into RDM. No redelivery.", e);
            callback.onError(converted.getRefBookCode(), addUpdate, delete, e);
        }
    }

}
