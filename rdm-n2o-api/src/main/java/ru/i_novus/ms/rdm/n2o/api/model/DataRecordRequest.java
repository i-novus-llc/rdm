package ru.i_novus.ms.rdm.n2o.api.model;

import ru.i_novus.ms.rdm.api.model.Structure;

/** Запрос на формирование метаданных в DataRecord*Provider'ах. */
public class DataRecordRequest {

    /** Идентификатор версии. */
    private Integer versionId;

    /** Структура версии. */
    private Structure structure;

    /** Тип действия, выполняемого над записью. */
    private String dataAction;

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public String getDataAction() {
        return dataAction;
    }

    public void setDataAction(String dataAction) {
        this.dataAction = dataAction;
    }
}

