package ru.inovus.ms.rdm.sync.service.change_data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.model.refdata.ChangeDataRequest;
import ru.inovus.ms.rdm.api.service.RefBookService;

import java.util.List;

@Service
public class SyncChangeDataClient implements ChangeDataClient {

    @Autowired
    private RefBookService refBookService;

    @Autowired
    private ChangeDataRequestCallback callback;

    @Override
    public void changeData(String refBookCode, List<Object> addUpdate, List<Object> delete) {
        ChangeDataRequest req = Utils.convertToChangeDataRequest(refBookCode, addUpdate, delete);
        try {
            refBookService.changeData(req);
            callback.onSuccess(refBookCode, addUpdate, delete);
        } catch (Exception e) {
            callback.onError(refBookCode, addUpdate, delete, e);
        }
    }
}
