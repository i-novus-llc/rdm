package ru.inovus.ms.rdm.n2o.provider;

import net.n2oapp.platform.i18n.Messages;
import net.n2oapp.platform.jaxrs.MessageExceptionMapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/*
* change the response error status to 400 (BAD_REQUEST)
* remove @UserExceptionMapper after migrating to the version 2.1 of n2o platform
* */
@Provider
public class UserExceptionMapper extends MessageExceptionMapper {

    public UserExceptionMapper(Messages messages) {
        super(messages);
    }

    @Override
    public Response.Status getStatus() {
        return Response.Status.BAD_REQUEST;
    }

}
