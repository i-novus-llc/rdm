package ru.i_novus.ms.rdm.sync.service;

import ru.i_novus.ms.rdm.sync.service.change_data.RdmChangeDataClient;

@SuppressWarnings({"squid:S3010", "squid:S1118"})
public class RdmSyncJobContext {

    private static RdmSyncDao dao;
    private static RdmChangeDataClient rdmChangeDataClient;
    private static int exportToRdmBatchSize;

    public RdmSyncJobContext(RdmSyncDao dao, RdmChangeDataClient rdmChangeDataClient, int exportToRdmBatchSize) {
        RdmSyncJobContext.dao = dao;
        RdmSyncJobContext.rdmChangeDataClient = rdmChangeDataClient;
        RdmSyncJobContext.exportToRdmBatchSize = exportToRdmBatchSize;
    }

    public static RdmSyncDao getDao() {
        return dao;
    }

    public static RdmChangeDataClient getRdmChangeDataClient() {
        return rdmChangeDataClient;
    }

    public static int getExportToRdmBatchSize() {
        return exportToRdmBatchSize;
    }

}
