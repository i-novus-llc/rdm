package ru.inovus.ms.rdm.esnsi.jobs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;
import ru.inovus.ms.rdm.esnsi.EsnsiSmevClient;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRevisionsCountResponseType;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.inovus.ms.rdm.esnsi.jobs.AbstractEsnsiDictionaryProcessingJob.MESSAGE_ID_KEY;
import static ru.inovus.ms.rdm.esnsi.jobs.JobTestUtils.*;

@RunWith(MockitoJUnitRunner.class)
public class GetRevisionsCountJobTest {

    @InjectMocks
    private GetRevisionsCountJob job;

    @Mock
    private EsnsiSmevClient smevClient;

    @Test
    public void test() throws Exception {
        GetClassifierRevisionsCountResponseType resp = new GetClassifierRevisionsCountResponseType();
        resp.setRevisionsCount(NUM_REVISIONS);
        when(smevClient.getResponse(any(), anyObject())).thenReturn(
            Map.entry(resp, EMPTY_IN)
        );
        JobExecutionContext ctx = ctx(GetRevisionsCountJob.class, Map.of(MESSAGE_ID_KEY, UUID.randomUUID().toString()));
        GetRevisionsCountJob spyJob = spy(job);
        spyJob.jobDataMap = ctx.getJobDetail().getJobDataMap();
        spyJob.execute0(ctx);
        assertJobExecutedWithArgs(spyJob, Map.of());
    }

}
