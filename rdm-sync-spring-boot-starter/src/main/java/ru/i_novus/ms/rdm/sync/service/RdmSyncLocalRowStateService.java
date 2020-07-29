package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RdmSyncLocalRowStateService {

    @Autowired
    private RdmSyncDao dao;

    /**
     * @param table Таблица с локальным записями
     * @param pk Поле, по которому можно идентифицировать записи
     * @param pv Уникальное значение записи в поле {@code pk}
     * @return Состояние записи в локальной таблице или null, если такой записи в таблице нет
     */
    public RdmSyncLocalRowState getLocalRowState(String table, String pk, Object pv) {
        return dao.getLocalRowState(table, pk, pv);
    }

}
