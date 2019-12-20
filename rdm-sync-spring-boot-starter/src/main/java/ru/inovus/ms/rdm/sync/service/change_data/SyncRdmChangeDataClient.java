package ru.inovus.ms.rdm.sync.service.change_data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.model.refdata.RdmChangeDataRequest;
import ru.inovus.ms.rdm.api.service.RefBookService;

import java.io.Serializable;
import java.util.List;

@Service
public class SyncRdmChangeDataClient extends RdmChangeDataClient {

    @Autowired private RdmChangeDataRequestCallback callback;
    @Autowired protected RefBookService refBookService;

    @Override
    public <T extends Serializable> void changeData(String refBookCode, List<? extends T> addUpdate, List<? extends T> delete) {
        RdmChangeDataRequest req = Utils.convertToRdmChangeDataRequest(refBookCode, addUpdate, delete);
        try {
            refBookService.changeData(req);
            callback.onSuccess(refBookCode, addUpdate, delete);
        } catch (Exception e) {
            callback.onError(refBookCode, addUpdate, delete, e);
        }
    }

}
