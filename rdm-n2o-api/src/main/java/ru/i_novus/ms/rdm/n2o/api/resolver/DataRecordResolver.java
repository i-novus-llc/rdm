package ru.i_novus.ms.rdm.n2o.api.resolver;

import java.io.Serializable;

public interface DataRecordResolver {

    boolean isSatisfied(String dataAction);

    Serializable resolve(String code, Serializable[] args);
}
