package ru.i_novus.ms.rdm.sync.service.change_data;

import net.n2oapp.platform.jaxrs.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.i_novus.ms.rdm.api.service.RefBookService;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class RdmChangeDataListener {

    private static final Logger logger = LoggerFactory.getLogger(RdmChangeDataListener.class);

    private static final Set<String> CONCURRENCY_ISSUES = Set.of("refbook.lock.draft.is.publishing", "refbook.lock.draft.is.updating", "refbook.lock.cannot-be-acquired");

    private final RefBookService refBookService;

    private final RdmChangeDataRequestCallback callback;

    public RdmChangeDataListener(RefBookService refBookService, RdmChangeDataRequestCallback callback) {
        this.refBookService = refBookService;
        this.callback = callback;
    }

    @JmsListener(destination = "${rdm_sync.change_data.queue:rdmChangeData}", containerFactory = "rdmChangeDataQueueMessageListenerContainerFactory")
    public <T extends Serializable> void onChangeDataRequest(List<Object> msg) {

        List<List<? extends T>> src = (List<List<? extends T>>) msg.get(0);
        List<? extends T> addUpdate = src.get(0);
        List<? extends T> delete = src.get(1);
        RdmChangeDataRequest converted = (RdmChangeDataRequest) msg.get(1);

        logger.info("Change data request on refBook with code {} arrived.", converted.getRefBookCode());
        try {
            refBookService.changeData(converted);
            callback.onSuccess(converted.getRefBookCode(), addUpdate, delete);

        } catch (RestException re) {
            boolean concurrencyIssue = false;
            if (re.getErrors() != null) {
                concurrencyIssue = re.getErrors().stream()
                        .filter(error -> error != null && error.getMessage() != null)
                        .anyMatch(error -> CONCURRENCY_ISSUES.contains(error.getMessage()));
            }
            if (re.getMessage() != null) {
                concurrencyIssue |= CONCURRENCY_ISSUES.contains(re.getMessage());
            }

            if (concurrencyIssue)
                throw new RdmException();

            callback.onError(converted.getRefBookCode(), addUpdate, delete, re);

        } catch (Exception e) {
            logger.error("Error occurred while pulling changes into RDM. No redelivery.", e);
            callback.onError(converted.getRefBookCode(), addUpdate, delete, e);
        }
    }

}
