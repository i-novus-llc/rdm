package ru.i_novus.ms.rdm.config;

import com.google.gson.Gson;
import net.n2oapp.framework.security.autoconfigure.userinfo.UserInfoModel;
import net.n2oapp.security.auth.common.OauthUser;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.apache.cxf.message.Message.PROTOCOL_HEADERS;

@Provider(Provider.Type.OutInterceptor)
public class UserInfoJaxRsInterceptor extends AbstractPhaseInterceptor<Message> {

    public UserInfoJaxRsInterceptor() {
        super(Phase.PREPARE_SEND);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        Object protocolHeaders = message.get(PROTOCOL_HEADERS);
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String json = new Gson().toJson(new UserInfoModel(((OauthUser) principal)));
        ((MetadataMap) protocolHeaders).put("n2o-user-info", List.of(json));
    }

    @Override
    public void handleFault(Message message) {

    }
}
