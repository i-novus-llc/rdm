package ru.i_novus.ms.rdm.config;

import com.google.gson.Gson;
import net.n2oapp.framework.security.autoconfigure.userinfo.UserInfoModel;
import net.n2oapp.security.auth.common.OauthUser;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import java.util.List;
import java.util.Map;

import static org.apache.cxf.annotations.Provider.Type.OutInterceptor;
import static org.apache.cxf.message.Message.PROTOCOL_HEADERS;

@Provider(OutInterceptor)
public class UserInfoCxfInterceptor extends AbstractPhaseInterceptor<Message> {

    public UserInfoCxfInterceptor() {
        super(Phase.PREPARE_SEND);
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        final Object principal = SecurityContextUtils.getPrincipal();
        if (principal == null) return;

        /* to-do: Add PrincipalToJsonAbstractMapper using ApplicationContextAware-implemented class.
           https://codippa.com/how-to-autowire-objects-in-non-spring-classes/
         */
        final String json = new Gson().toJson(new UserInfoModel(((OauthUser) principal)));

        final Map<String, List<String>> headers = CastUtils.cast((Map)message.get(PROTOCOL_HEADERS));
        headers.put("n2o-user-info", List.of(json));
    }

    @Override
    public void handleFault(Message message) {
        handleMessage(message);
    }
}
