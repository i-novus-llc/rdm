package ru.inovus.ms.rdm.sync.service.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import ru.inovus.ms.rdm.api.model.refdata.ChangeDataRequest;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;

public class PullInRdmListener {

    private static final Logger logger = LoggerFactory.getLogger(PullInRdmListener.class);

    private RdmSyncRest rdmSyncRest;

    public PullInRdmListener(RdmSyncRest rdmSyncRest) {
        this.rdmSyncRest = rdmSyncRest;
    }

    @JmsListener(destination = "${rdm_sync.pull_in_rdm.queue:pullInRdm}", containerFactory = "pullInRdmQueueMessageListenerContainerFactory")
    public void onPullRequest(ChangeDataRequest changeDataRequest) {
        logger.info("Pull request on refBook with code {} arrived.", changeDataRequest.getRefBookCode());
        rdmSyncRest.pullInRdm(changeDataRequest);
    }

}
