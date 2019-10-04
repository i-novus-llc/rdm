package ru.inovus.ms.rdm.util;

import net.n2oapp.framework.access.simple.PermissionApi;
import net.n2oapp.framework.api.user.StaticUserContext;
import net.n2oapp.framework.api.user.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import ru.inovus.ms.rdm.api.util.RdmPermission;

public class RdmPermissionImpl implements RdmPermission {

    private static final String PERMISSION_NSI_EDIT = "nsi.edit";
    private static final String PERMISSION_NSI_ARCHIVE = "nsi.archive";

    private UserContext userContext;

    @Autowired
    private PermissionApi permissionApi;

    public RdmPermissionImpl() {
        this.userContext = StaticUserContext.getUserContext();
    }

    public UserContext getUserContext() {
        return userContext;
    }

    public void setUserContext(UserContext userContext) {
        this.userContext = userContext;
    }

    public PermissionApi getPermissionApi() {
        return permissionApi;
    }

    public void setPermissionApi(PermissionApi permissionApi) {
        this.permissionApi = permissionApi;
    }

    // Исключение черновика из списка версий справочника.
    @Override
    public boolean excludeDraft() {
        return !permissionApi.hasPermission(userContext, PERMISSION_NSI_EDIT)
                && !permissionApi.hasPermission(userContext, PERMISSION_NSI_ARCHIVE);
    }
}
