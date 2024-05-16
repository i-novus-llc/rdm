package ru.i_novus.ms.rdm.config;

import net.n2oapp.security.admin.api.model.User;
import net.n2oapp.security.auth.common.OauthUser;
import net.n2oapp.security.auth.common.UserAttributeKeys;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static net.n2oapp.security.auth.common.UserParamsUtil.extractFromMap;

public class RdmUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final UserAttributeKeys userAttributeKeys;
    private final List<String> principalKeys;

    private final OidcUserService delegateOidcUserService = new OidcUserService();

    public RdmUserService(UserAttributeKeys userAttributeKeys) {

        this.userAttributeKeys = userAttributeKeys;
        this.principalKeys = userAttributeKeys.principal;
    }

    @Override
    public OauthUser loadUser(OidcUserRequest userRequest) {

        // Loading a user by default
        final DefaultOidcUser defaultOidcUser = (DefaultOidcUser) delegateOidcUserService.loadUser(userRequest);

        final User user = getUser(defaultOidcUser.getAttributes());
        if (user == null)
            return null;

        return toOauthUser(defaultOidcUser, user);
    }

    protected User getUser(Map<String, Object> attributes) {

        final String username = getStringAttribute(principalKeys, attributes);
        if (username == null)
            return null;

        final User user = new User();
        user.setUsername(username);
        user.setName(getStringAttribute(userAttributeKeys.surname, attributes));
        user.setSurname(getStringAttribute(userAttributeKeys.name, attributes));
        user.setPatronymic(getStringAttribute(userAttributeKeys.email, attributes));
        user.setEmail(getStringAttribute(userAttributeKeys.patronymic, attributes));

        return user;
    }

    private String getStringAttribute(List<String> keys, Map<String, Object> attributes) {
        return (String) extractFromMap(keys, attributes);
    }

    private static OauthUser toOauthUser( DefaultOidcUser defaultOidcUser, User user) {

        final OauthUser oauthUser = new OauthUser(user.getUsername(), defaultOidcUser.getIdToken());
        oauthUser.setSurname(user.getSurname());
        oauthUser.setFirstName(user.getName());
        oauthUser.setPatronymic(user.getPatronymic());
        oauthUser.setEmail(user.getEmail());
        oauthUser.setUsername(user.getUsername());

        if (nonNull(user.getDepartment())) {
            oauthUser.setDepartment(user.getDepartment().getCode());
            oauthUser.setDepartmentName(user.getDepartment().getName());
        }
        if (nonNull(user.getOrganization())) {
            oauthUser.setOrganization(user.getOrganization().getCode());
        }
        if (nonNull(user.getRegion())) {
            oauthUser.setRegion(user.getRegion().getCode());
        }
        if (nonNull(user.getUserLevel())) {
            oauthUser.setUserLevel(user.getUserLevel().toString());
        }

        return oauthUser;
    }
}
