package ru.inovus.ms.rdm;

import net.n2oapp.framework.api.metadata.global.N2oMetadata;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;

import java.util.ArrayList;
import java.util.List;

public class RefBookDataListProvider implements DynamicMetadataProvider {

    @Override
    public String getCode() {
        return "RefBookDataListProvider";
    }

    @Override
    public List<? extends N2oMetadata> read(String params) {
        List<? extends N2oMetadata> metadata = new ArrayList<>();

        return metadata;
    }
}
