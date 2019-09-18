package ru.inovus.ms.rdm.n2o.util;

import net.n2oapp.framework.access.simple.PermissionApi;
import net.n2oapp.framework.api.user.UserContext;

public class RdmPermission {

    private static final String PERMISSION_NSI_READ = "nsi.edit";
    private static final String PERMISSION_NSI_ARCHIVE = "nsi.archive";

    private UserContext userContext;
    private PermissionApi permissionApi;

    public RdmPermission(UserContext userContext, PermissionApi permissionApi) {
        this.userContext = userContext;
        this.permissionApi = permissionApi;
    }

    // Исключение черновика из списка версий справочника.
    public boolean excludeDraft() {
        return !permissionApi.hasPermission(userContext, PERMISSION_NSI_READ)
                && !permissionApi.hasPermission(userContext, PERMISSION_NSI_ARCHIVE);
    }
}
