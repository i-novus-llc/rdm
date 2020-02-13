package ru.inovus.ms.rdm.rest.loader;

import net.n2oapp.platform.loader.server.ServerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.RefBookService;

import java.util.List;

import static org.apache.cxf.common.util.CollectionUtils.isEmpty;

@Component
public class RefBookDataServerLoader implements ServerLoader<FileModel> {

    private static final Logger logger = LoggerFactory.getLogger(RefBookDataServerLoader.class);

    @Autowired
    @Qualifier("refBookServiceJaxRsProxyClient")
    private RefBookService refBookService;

    @Autowired
    @Qualifier("publishServiceJaxRsProxyClient")
    private PublishService publishService;

    @Override
    public String getTarget() {
        return "dictionaryData";
    }

    @Override
    public Class<FileModel> getDataType() {
        return FileModel.class;
    }

    @Override
    public void load(List<FileModel> data, String subject) {

        if (isEmpty(data))
            return;

        data.forEach(this::createAndPublishRefBook);

        logger.info("subject = {}", subject);
    }

    private void createAndPublishRefBook(FileModel fileModel) {

        Draft draft = refBookService.create(fileModel);
        publishService.publish(draft.getId(), null, null, null, false);
    }
}
