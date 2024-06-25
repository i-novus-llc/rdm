package ru.i_novus.ms.rdm.n2o.util;

import net.n2oapp.framework.access.simple.PermissionApi;
import net.n2oapp.framework.api.user.StaticUserContext;
import net.n2oapp.framework.api.user.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.i_novus.ms.rdm.api.util.RdmPermission;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;

public class RdmPermissionImpl implements RdmPermission {

    private static final String ANY_PERMISSION = "*";

    @Value("${rdm.permissions.draft.version}")
    private List<String> rdmPermissionsDraftVersion;

    private final UserContext userContext;

    @Autowired
    private PermissionApi permissionApi;

    public RdmPermissionImpl() {
        this.userContext = StaticUserContext.getUserContext();
    }

    // Исключение черновика из списка версий справочника.
    @Override
    public boolean excludeDraft() {
        return isEmpty(rdmPermissionsDraftVersion) ||
                rdmPermissionsDraftVersion.stream()
                        .noneMatch(permission ->
                                ANY_PERMISSION.equals(permission)
                                        || permissionApi.hasPermission(userContext, permission)
                        );
    }
}
