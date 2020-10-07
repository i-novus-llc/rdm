package ru.i_novus.ms.rdm.n2o.api.service;

import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;

import java.util.List;

/** Декоратор работы с данными справочника. */
public interface RefBookDataDecorator {

    Structure getDataStructure(Integer versionId, DataCriteria criteria);

    List<RefBookRowValue> getDataContent(List<RefBookRowValue> searchContent, DataCriteria criteria);
}
