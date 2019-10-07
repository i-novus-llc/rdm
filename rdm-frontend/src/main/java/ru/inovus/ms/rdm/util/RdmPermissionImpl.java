package ru.inovus.ms.rdm.util;

import net.n2oapp.framework.access.simple.PermissionApi;
import net.n2oapp.framework.api.user.StaticUserContext;
import net.n2oapp.framework.api.user.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.inovus.ms.rdm.api.util.RdmPermission;

import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class RdmPermissionImpl implements RdmPermission {

    private static final Pattern PERMISSION_SPLITTER = Pattern.compile(",");

    @Value("${rdm.permissions.nsi.draft.version}")
    private String rdmPermissionsNsiDraftVersion;

    private UserContext userContext;

    @Autowired
    private PermissionApi permissionApi;

    public RdmPermissionImpl() {
        this.userContext = StaticUserContext.getUserContext();
    }

    // Исключение черновика из списка версий справочника.
    @Override
    public boolean excludeDraft() {
        return isEmpty(rdmPermissionsNsiDraftVersion) ||
                PERMISSION_SPLITTER.splitAsStream(rdmPermissionsNsiDraftVersion)
                        .map(String::trim)
                        .filter(value -> !isEmpty(value))
                        .noneMatch(permission -> permissionApi.hasPermission(userContext, permission));
    }
}
