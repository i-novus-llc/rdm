package ru.inovus.ms.rdm.n2o.util;

import ru.inovus.ms.rdm.api.util.RdmPermission;

public class RdmPermissionImpl implements RdmPermission {

    public RdmPermissionImpl() {
        // nothing to do
    }

    // Исключение черновика из списка версий справочника.
    @Override
    public boolean excludeDraft() {
        return false;
    }
}
